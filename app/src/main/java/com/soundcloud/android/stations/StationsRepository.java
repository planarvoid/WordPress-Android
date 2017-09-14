package com.soundcloud.android.stations;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.utils.DiffUtils;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.collections.Lists;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.functions.Function;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class StationsRepository {

    static final Function<StationRecord, StationRecord> IDENTITY = s -> s;

    private final StationsStorage stationsStorage;
    private final Scheduler scheduler;
    private final StationsApi stationsApi;
    private final StoreTracksCommand storeTracksCommand;
    private final StoreStationCommand storeStationCommand;
    private final SyncStateStorage syncStateStorage;
    private final SyncInitiator syncInitiator;

    @Inject
    public StationsRepository(StationsStorage stationsStorage,
                              @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler,
                              StationsApi stationsApi,
                              StoreTracksCommand storeTracksCommand,
                              StoreStationCommand storeStationCommand,
                              SyncStateStorage syncStateStorage,
                              SyncInitiator syncInitiator) {
        this.stationsStorage = stationsStorage;
        this.scheduler = scheduler;
        this.stationsApi = stationsApi;
        this.storeTracksCommand = storeTracksCommand;
        this.storeStationCommand = storeStationCommand;
        this.syncStateStorage = syncStateStorage;
        this.syncInitiator = syncInitiator;
    }

    public Maybe<StationRecord> station(Urn station, Function<StationRecord, StationRecord> stationMapper) {
        return stationsStorage.station(station)
                              .filter(stationRecord -> stationRecord.getTracks().size() > 0)
                              .switchIfEmpty(syncSingleStation(station, stationMapper).toMaybe());
    }

    public Maybe<StationRecord> station(Urn station) {
        return station(station, IDENTITY);
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

    public Single<List<StationMetadata>> stationsMetadata(List<Urn> urns) {
        return stationsStorage.loadStationsMetadata(urns)
                              .flatMap(result -> syncMissingStationsMetadata(urns, result));
    }

    private Single<List<StationMetadata>> syncMissingStationsMetadata(List<Urn> requested, List<StationMetadata> obtained) {
        if (requested.size() == obtained.size()) {
            return Single.just(obtained);
        } else {
            final List<Urn> stationsToSync = DiffUtils.minus(requested, Lists.transform(obtained, StationMetadata::urn));
            return syncStationsMetadata(stationsToSync)
                    .andThen(stationsStorage.loadStationsMetadata(requested))
                    .onErrorReturnItem(obtained);
        }
    }

    public Completable syncStationsMetadata(List<Urn> urns) {
        return Single.fromCallable(() -> stationsApi.fetchStations(urns))
                     .doOnSuccess(stationsStorage::storeStationsMetadata)
                     .doOnError(ErrorUtils::handleSilentException) //make sure we don't swallow the error
                     .toCompletable();
    }

    Single<List<StationRecord>> loadStationsCollection(final int type) {
        return stationsStorage.getStationsCollection(type).subscribeOn(scheduler);
    }

    private Single<List<StationRecord>> syncAndLoadStationsCollection(int type) {
        return syncStations(type).flatMap(__ -> loadStationsCollection(type));
    }

    Single<SyncJobResult> syncStations(int type) {
        return syncInitiator.sync(typeToSyncable(type));
    }

    Single<List<StationTrack>> loadStationPlayQueue(Urn station, int startPosition) {
        return syncSingleStation(station, IDENTITY)
                .flatMap(__ -> stationsStorage.loadStationPlayQueue(station, startPosition));
    }

    void saveStationLastPlayedTrackPosition(Urn collectionUrn, int position) {
        stationsStorage.saveLastPlayedTrackPosition(collectionUrn, position);
    }

    void clearData() {
        stationsStorage.clear();
    }

    Completable clearExpiredPlayQueue(Urn station) {
        return stationsStorage.clearExpiredPlayQueue(station);
    }

    Completable updateLocalStationLike(Urn stationUrn, boolean liked) {
        return stationsStorage.updateLocalStationLike(stationUrn, liked);
    }

    Maybe<StationWithTrackUrns> stationWithTrackUrns(final Urn station, Function<StationRecord, StationRecord> stationMapper) {
        return stationsStorage.stationWithTrackUrns(station)
                              .filter(stationWithTrackUrns -> stationWithTrackUrns.trackUrns().size() > 0)
                              .switchIfEmpty(syncSingleStation(station, stationMapper).flatMapMaybe(o -> stationsStorage.stationWithTrackUrns(station)));
    }

    Single<StationRecord> syncSingleStation(Urn station, Function<StationRecord, StationRecord> stationMapper) {
        return stationsApi.fetchStation(station)
                          .doOnSuccess(this::storeStationTracks)
                          .map(stationMapper)
                          .doOnSuccess(this::storeStation);
    }

    private void storeStationTracks(ApiStation apiStation) {
        storeTracksCommand.call(apiStation.getTrackRecords());
    }

    private void storeStation(StationRecord stationRecord) {
        storeStationCommand.call(stationRecord);
    }

    private Syncable typeToSyncable(int type) {
        switch (type) {
            case StationsCollectionsTypes.LIKED:
                return Syncable.LIKED_STATIONS;
            default:
                throw new IllegalArgumentException("Unknown station's type: " + type);
        }
    }

    void saveRecentlyPlayedStation(Urn stationUrn) {
        stationsStorage.saveUnsyncedRecentlyPlayedStation(stationUrn);
    }

}

