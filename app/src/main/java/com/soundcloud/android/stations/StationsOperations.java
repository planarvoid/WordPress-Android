package com.soundcloud.android.stations;

import static com.soundcloud.android.events.EventQueue.URN_STATE_CHANGED;
import static com.soundcloud.android.events.UrnStateChangedEvent.fromStationsUpdated;
import static com.soundcloud.android.playback.PlaySessionSource.forStation;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.rx.observers.DefaultCompletableObserver;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.functions.Function;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class StationsOperations {

    private final StationsRepository stationsRepository;
    private final Scheduler scheduler;
    private final EventBusV2 eventBus;
    private final TrackItemRepository trackItemRepository;

    @Inject
    public StationsOperations(StationsRepository stationsRepository,
                              @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler,
                              EventBusV2 eventBus,
                              TrackItemRepository trackItemRepository) {
        this.stationsRepository = stationsRepository;
        this.scheduler = scheduler;
        this.eventBus = eventBus;
        this.trackItemRepository = trackItemRepository;
    }

    public Maybe<StationRecord> station(Urn station) {
        return stationsRepository
                .clearExpiredPlayQueue(station)
                .andThen(stationsRepository.station(station))
                .subscribeOn(scheduler);
    }

    public void toggleStationLikeAndForget(Urn stationUrn, boolean liked) {
        toggleStationLike(stationUrn, liked).subscribeWith(new DefaultCompletableObserver());
    }

    Maybe<StationWithTracks> stationWithTracks(Urn station, final Optional<Urn> seed) {
        return stationWithTracks(station, seed.isPresent() ? prependSeed(seed.get()) : stationRecord -> stationRecord);
    }

    private Maybe<StationWithTracks> stationWithTracks(Urn station, Function<StationRecord, StationRecord> toStation) {
        return stationsRepository.clearExpiredPlayQueue(station)
                                 .andThen(loadStationWithTracks(station, toStation))
                                 .subscribeOn(scheduler);
    }

    private Maybe<StationWithTracks> loadStationWithTracks(Urn station, Function<StationRecord, StationRecord> stationMapper) {
        return stationsRepository.stationWithTrackUrns(station, stationMapper)
                                 .flatMapSingleElement(entity -> trackItemRepository.trackListFromUrns(entity.trackUrns())
                                                                                    .map(tracks -> Lists.transform(tracks, StationInfoTrack::from))
                                                                                    .map(stationInfoTracks -> StationWithTracks.from(entity, stationInfoTracks)));
    }

    private Function<StationRecord, StationRecord> prependSeed(final Urn seed) {
        return station -> station.getTracks().isEmpty() ? station : Station.stationWithSeedTrack(station, seed);
    }

    public Single<List<StationRecord>> collection(final int type) {
        return stationsRepository.collection(type);
    }

    public Single<SyncJobResult> syncStations(int type) {
        return stationsRepository.syncStations(type);
    }

    Single<SyncJobResult> syncLikedStations() {
        return stationsRepository.syncStations(StationsCollectionsTypes.LIKED);
    }

    void saveLastPlayedTrackPosition(Urn collectionUrn, int position) {
        stationsRepository.saveStationLastPlayedTrackPosition(collectionUrn, position);
    }

    void saveRecentlyPlayedStation(Urn stationUrn) {
        stationsRepository.saveRecentlyPlayedStation(stationUrn);
    }

    Completable toggleStationLike(Urn stationUrn, boolean liked) {
        return stationsRepository.updateLocalStationLike(stationUrn, liked)
                                 .doOnComplete(eventBus.publishAction0(URN_STATE_CHANGED, fromStationsUpdated(stationUrn)))
                                 .subscribeOn(scheduler);
    }

    public Single<PlayQueue> fetchUpcomingTracks(final Urn station,
                                                 final int currentSize,
                                                 final PlaySessionSource playSessionSource) {

        final PlaySessionSource discoverySource = forStation(playSessionSource.getOriginScreen(),
                                                             playSessionSource.getCollectionOwnerUrn(),
                                                             DiscoverySource.STATIONS_SUGGESTIONS);

        return stationsRepository.loadStationPlayQueue(station, currentSize)
                                 .map(tracks -> PlayQueue.fromStation(station, tracks, discoverySource))
                                 .subscribeOn(scheduler);
    }

    public void clearData() {
        stationsRepository.clearData();
    }
}
