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
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.internal.util.UtilityFunctions;

import javax.inject.Inject;
import javax.inject.Named;

public class StationsOperations {

    private final Action1<ApiStation> storeTracks = new Action1<ApiStation>() {
        @Override
        public void call(ApiStation apiStation) {
            storeTracksCommand.call(apiStation.getTrackRecords());
        }
    };

    private final SyncStateStorage syncStateStorage;
    private final StationsStorage stationsStorage;
    private final StationsApi stationsApi;
    private final StoreTracksCommand storeTracksCommand;
    private final StoreStationCommand storeStationCommand;
    private final SyncInitiator syncInitiator;
    private final Scheduler scheduler;
    private final io.reactivex.Scheduler schedulerV2;
    private final EventBus eventBus;
    private final TrackItemRepository trackItemRepository;

    @Inject
    public StationsOperations(SyncStateStorage syncStateStorage,
                              StationsStorage stationsStorage,
                              StationsApi stationsApi,
                              StoreTracksCommand storeTracksCommand,
                              StoreStationCommand storeStationCommand,
                              SyncInitiator syncInitiator,
                              @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                              @Named(ApplicationModule.RX_HIGH_PRIORITY) io.reactivex.Scheduler schedulerV2,
                              EventBus eventBus,
                              TrackItemRepository trackItemRepository) {
        this.syncStateStorage = syncStateStorage;
        this.stationsStorage = stationsStorage;
        this.stationsApi = stationsApi;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
        this.storeTracksCommand = storeTracksCommand;
        this.storeStationCommand = storeStationCommand;
        this.schedulerV2 = schedulerV2;
        this.eventBus = eventBus;
        this.trackItemRepository = trackItemRepository;
    }

    public Observable<StationRecord> station(Urn station) {
        return stationsStorage
                .clearExpiredPlayQueue(station)
                .flatMap(o -> getStation(station, UtilityFunctions.identity()))
                .subscribeOn(scheduler);
    }

    Observable<StationWithTracks> stationWithTracks(Urn station, final Optional<Urn> seed) {
        return stationWithTracks(station, seed.isPresent() ? prependSeed(seed.get()) : UtilityFunctions.identity());
    }

    private Observable<StationWithTracks> stationWithTracks(Urn station, Func1<StationRecord, StationRecord> toStation) {
        return stationsStorage
                .clearExpiredPlayQueue(station)
                .flatMap(o -> loadStationWithTracks(station, toStation))
                .subscribeOn(scheduler);
    }

    private Observable<StationRecord> getStation(Urn station, Func1<StationRecord, StationRecord> toStation) {
        return Observable
                .concat(stationsStorage.station(station)
                                       .filter(stationFromStorage -> stationFromStorage != null && stationFromStorage.getTracks().size() > 0),
                        syncSingleStation(station, toStation)
                )
                .first();
    }

    private Observable<StationWithTracks> loadStationWithTracks(Urn station, Func1<StationRecord, StationRecord> toStation) {
        return Observable.concat(loadStationWithTracks(station),
                                 syncSingleStation(station, toStation).flatMap(o -> loadStationWithTracks(station)))
                         .first();
    }

    private Observable<StationWithTracks> loadStationWithTracks(Urn station) {
        return stationsStorage.stationWithTrackUrns(station)
                              .filter(stationFromStorage -> stationFromStorage != null && stationFromStorage.trackUrns().size() > 0)
                              .flatMap(entity -> trackItemRepository.trackListFromUrns(entity.trackUrns())
                                                                    .map(tracks -> Lists.transform(tracks, StationInfoTrack::from))
                                                                    .map(stationInfoTracks -> StationWithTracks.from(entity, stationInfoTracks)));
    }

    private Observable<StationRecord> syncSingleStation(Urn station, Func1<StationRecord, StationRecord> toStation) {
        return stationsApi.fetchStation(station)
                          .doOnNext(storeTracks)
                          .map(toStation)
                          .doOnNext(storeStationCommand.toAction1());
    }

    private Func1<StationRecord, StationRecord> prependSeed(final Urn seed) {
        return station -> station.getTracks().isEmpty() ? station : Station.stationWithSeedTrack(station, seed);
    }

    public io.reactivex.Observable<StationRecord> collection(final int type) {
        final io.reactivex.Observable<StationRecord> collection;
        if (syncStateStorage.hasSyncedBefore(typeToSyncable(type))) {
            collection = loadStationsCollection(type);
        } else {
            collection = syncAndLoadStationsCollection(type);
        }
        return collection.subscribeOn(schedulerV2);
    }

    private io.reactivex.Observable<StationRecord> loadStationsCollection(final int type) {
        return RxJava.toV2Observable(stationsStorage.getStationsCollection(type).subscribeOn(scheduler));
    }

    private io.reactivex.Observable<StationRecord> syncAndLoadStationsCollection(int type) {
        return syncStations(type).flatMapObservable(__ -> loadStationsCollection(type));
    }

    public io.reactivex.Single<SyncJobResult> syncStations(int type) {
        return syncInitiator.sync(typeToSyncable(type));
    }

    Observable<SyncJobResult> syncLikedStations() {
        return RxJava.toV1Observable(syncInitiator.sync(Syncable.LIKED_STATIONS));
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

    Observable<ChangeResult> toggleStationLike(Urn stationUrn, boolean liked) {
        return stationsStorage.updateLocalStationLike(stationUrn, liked)
                              .doOnNext(eventBus.publishAction1(URN_STATE_CHANGED, fromStationsUpdated(stationUrn)))
                              .subscribeOn(scheduler);
    }

    public Observable<PlayQueue> fetchUpcomingTracks(final Urn station,
                                                     final int currentSize,
                                                     final PlaySessionSource playSessionSource) {
        final PlaySessionSource discoverySource = forStation(playSessionSource.getOriginScreen(),
                                                             playSessionSource.getCollectionOwnerUrn(),
                                                             DiscoverySource.STATIONS_SUGGESTIONS);
        return stationsApi
                .fetchStation(station)
                .doOnNext(storeTracks)
                .doOnNext(storeStationCommand.toAction1())
                .flatMap(ignored -> stationsStorage.loadPlayQueue(station, currentSize))
                .toList()
                .map(tracks -> PlayQueue.fromStation(station, tracks, discoverySource))
                .subscribeOn(scheduler);
    }

    public void clearData() {
        stationsStorage.clear();
    }
}
