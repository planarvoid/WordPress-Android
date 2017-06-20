package com.soundcloud.android.stations;

import static com.soundcloud.android.events.EventQueue.URN_STATE_CHANGED;
import static com.soundcloud.android.events.UrnStateChangedEvent.fromStationsUpdated;
import static com.soundcloud.android.playback.PlaySessionSource.forStation;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.rx.observers.DefaultSingleObserver;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class StationsOperations {

    private final SyncStateStorage syncStateStorage;
    private final StationsStorage stationsStorage;
    private final StationsApi stationsApi;
    private final StoreTracksCommand storeTracksCommand;
    private final StoreStationCommand storeStationCommand;
    private final SyncInitiator syncInitiator;
    private final Scheduler scheduler;
    private final EventBusV2 eventBus;
    private final TrackItemRepository trackItemRepository;

    @Inject
    public StationsOperations(SyncStateStorage syncStateStorage,
                              StationsStorage stationsStorage,
                              StationsApi stationsApi,
                              StoreTracksCommand storeTracksCommand,
                              StoreStationCommand storeStationCommand,
                              SyncInitiator syncInitiator,
                              @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler,
                              EventBusV2 eventBus,
                              TrackItemRepository trackItemRepository) {
        this.syncStateStorage = syncStateStorage;
        this.stationsStorage = stationsStorage;
        this.stationsApi = stationsApi;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
        this.storeTracksCommand = storeTracksCommand;
        this.storeStationCommand = storeStationCommand;
        this.eventBus = eventBus;
        this.trackItemRepository = trackItemRepository;
    }

    public Maybe<StationRecord> station(Urn station) {
        return stationsStorage
                .clearExpiredPlayQueue(station)
                .flatMapMaybe(__ -> getStation(station, stationRecord -> stationRecord))
                .subscribeOn(scheduler);
    }

    public Disposable toggleStationLikeAndForget(Urn stationUrn, boolean liked) {
        return toggleStationLike(stationUrn, liked).subscribeWith(new DefaultSingleObserver<>());
    }

    Maybe<StationWithTracks> stationWithTracks(Urn station, final Optional<Urn> seed) {
        return stationWithTracks(station, seed.isPresent() ? prependSeed(seed.get()) : stationRecord -> stationRecord);
    }

    private Maybe<StationWithTracks> stationWithTracks(Urn station, Function<StationRecord, StationRecord> toStation) {
        return stationsStorage.clearExpiredPlayQueue(station)
                              .flatMapMaybe(__ -> loadStationWithTracks(station, toStation))
                              .subscribeOn(scheduler);
    }

    private Maybe<StationRecord> getStation(Urn station, Function<StationRecord, StationRecord> toStation) {
        return Maybe
                .concat(stationsStorage.station(station)
                                       .filter(stationFromStorage -> stationFromStorage != null && stationFromStorage.getTracks().size() > 0),
                        syncSingleStation(station, toStation).toMaybe()
                )
                .firstElement();
    }

    private Maybe<StationWithTracks> loadStationWithTracks(Urn station, Function<StationRecord, StationRecord> toStation) {
        return Maybe.concat(loadStationWithTracks(station),
                            syncSingleStation(station, toStation).flatMapMaybe(__ -> loadStationWithTracks(station)))
                    .firstElement();
    }

    private Maybe<StationWithTracks> loadStationWithTracks(Urn station) {
        return stationsStorage.stationWithTrackUrns(station)
                              .filter(stationFromStorage -> stationFromStorage.trackUrns().size() > 0)
                              .flatMap(entity -> trackItemRepository.trackListFromUrns(entity.trackUrns())
                                                                    .map(tracks -> Lists.transform(tracks, StationInfoTrack::from))
                                                                    .map(stationInfoTracks -> StationWithTracks.from(entity, stationInfoTracks))
                                                                    .toMaybe());
    }

    private Single<StationRecord> syncSingleStation(Urn station, Function<StationRecord, StationRecord> toStation) {
        return stationsApi.fetchStation(station)
                          .doOnSuccess(apiStation -> storeTracksCommand.call(apiStation.getTrackRecords()))
                          .map(toStation)
                          .doOnSuccess(storeStationCommand.toConsumer());
    }

    private Function<StationRecord, StationRecord> prependSeed(final Urn seed) {
        return station -> station.getTracks().isEmpty() ? station : Station.stationWithSeedTrack(station, seed);
    }

    public Single<List<StationRecord>> collection(final int type) {
        final Single<List<StationRecord>> collection;
        if (syncStateStorage.hasSyncedBefore(typeToSyncable(type))) {
            collection = loadStationsCollection(type);
        } else {
            collection = syncAndLoadStationsCollection(type);
        }
        return collection.subscribeOn(scheduler);
    }

    private Single<List<StationRecord>> loadStationsCollection(final int type) {
        return stationsStorage.getStationsCollection(type)
                              .subscribeOn(scheduler);
    }

    private Single<List<StationRecord>> syncAndLoadStationsCollection(int type) {
        return syncStations(type).flatMap(__ -> loadStationsCollection(type));
    }

    public Single<SyncJobResult> syncStations(int type) {
        return syncInitiator.sync(typeToSyncable(type));
    }

    Single<SyncJobResult> syncLikedStations() {
        return syncInitiator.sync(Syncable.LIKED_STATIONS);
    }

    ChangeResult saveLastPlayedTrackPosition(Urn collectionUrn, int position) {
        return stationsStorage.saveLastPlayedTrackPosition(collectionUrn, position);
    }

    private Syncable typeToSyncable(int type) {
        switch (type) {
            case StationsCollectionsTypes.LIKED:
                return Syncable.LIKED_STATIONS;
            case StationsCollectionsTypes.RECOMMENDATIONS:
                return Syncable.RECOMMENDED_STATIONS;
            default:
                throw new IllegalArgumentException("Unknown station's type: " + type);
        }
    }

    ChangeResult saveRecentlyPlayedStation(Urn stationUrn) {
        final ChangeResult result = stationsStorage.saveUnsyncedRecentlyPlayedStation(stationUrn);
        syncInitiator.requestSystemSync();
        return result;
    }

    Single<ChangeResult> toggleStationLike(Urn stationUrn, boolean liked) {
        return stationsStorage.updateLocalStationLike(stationUrn, liked)
                              .doOnSuccess(eventBus.publishAction1(URN_STATE_CHANGED, fromStationsUpdated(stationUrn)))
                              .subscribeOn(scheduler);
    }

    public Single<PlayQueue> fetchUpcomingTracks(final Urn station,
                                                 final int currentSize,
                                                 final PlaySessionSource playSessionSource) {
        final PlaySessionSource discoverySource = forStation(playSessionSource.getOriginScreen(),
                                                             playSessionSource.getCollectionOwnerUrn(),
                                                             DiscoverySource.STATIONS_SUGGESTIONS);
        return stationsApi
                .fetchStation(station)
                .doOnSuccess(apiStation -> storeTracksCommand.call(apiStation.getTrackRecords()))
                .doOnSuccess(storeStationCommand.toConsumer())
                .flatMap(__ -> stationsStorage.loadPlayQueue(station, currentSize))
                .map(tracks -> PlayQueue.fromStation(station, tracks, discoverySource))
                .subscribeOn(scheduler);
    }

    public void clearData() {
        stationsStorage.clear();
    }
}
