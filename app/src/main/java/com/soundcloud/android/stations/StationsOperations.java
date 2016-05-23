package com.soundcloud.android.stations;

import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.propeller.ChangeResult;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.internal.util.UtilityFunctions;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class StationsOperations {
    private final SyncStateStorage syncStateStorage;
    private final StationsStorage stationsStorage;
    private final StationsApi stationsApi;
    private final StoreTracksCommand storeTracksCommand;
    private final StoreStationCommand storeStationCommand;
    private final StationsSyncInitiator syncInitiator;
    private final Scheduler scheduler;

    private final Action1<ApiStation> storeTracks = new Action1<ApiStation>() {
        @Override
        public void call(ApiStation apiStation) {
            storeTracksCommand.call(apiStation.getTrackRecords());
        }
    };

    private static final Func1<StationRecord, Boolean> HAS_TRACKS = new Func1<StationRecord, Boolean>() {
        @Override
        public Boolean call(StationRecord station) {
            return station.getTracks().size() > 0;
        }
    };

    @Inject
    public StationsOperations(SyncStateStorage syncStateStorage,
                              StationsStorage stationsStorage,
                              StationsApi stationsApi,
                              StoreTracksCommand storeTracksCommand,
                              StoreStationCommand storeStationCommand,
                              StationsSyncInitiator syncInitiator,
                              @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.syncStateStorage = syncStateStorage;
        this.stationsStorage = stationsStorage;
        this.stationsApi = stationsApi;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
        this.storeTracksCommand = storeTracksCommand;
        this.storeStationCommand = storeStationCommand;
    }

    public Observable<StationRecord> station(Urn station) {
        return station(station, UtilityFunctions.<StationRecord>identity());
    }

    Observable<StationRecord> stationWithSeed(Urn station, final Urn seed) {
        return station(station, prependSeed(seed));
    }

    private Observable<StationRecord> station(Urn station, Func1<StationRecord, StationRecord> toStation) {
        return stationsStorage
                .clearExpiredPlayQueue(station)
                .flatMap(continueWith(loadStation(station, toStation)))
                .subscribeOn(scheduler);
    }

    private Observable<StationRecord> loadStation(Urn station, Func1<StationRecord, StationRecord> toStation) {
        return Observable
                .concat(stationsStorage.station(station).filter(HAS_TRACKS),
                        stationsApi
                                .fetchStation(station)
                                .doOnNext(storeTracks)
                                .map(toStation)
                                .doOnNext(storeStationCommand.toAction1())
                )
                .first();
    }

    private Func1<StationRecord, StationRecord> prependSeed(final Urn seed) {
        return new Func1<StationRecord, StationRecord>() {
            @Override
            public StationRecord call(StationRecord station) {
                if (station.getTracks().isEmpty()) {
                    return station;
                }
                return Station.stationWithSeedTrack(station, seed);
            }
        };
    }

    public Observable<StationRecord> collection(final int type) {
        final Observable<StationRecord> collection;
        if (syncStateStorage.hasSyncedBefore(StationsSyncInitiator.TYPE)) {
            collection = loadStationsCollection(type);
        } else {
            collection = syncAndLoadStationsCollection(type);
        }
        return collection.subscribeOn(scheduler);
    }

    private Observable<StationRecord> loadStationsCollection(final int type) {
        return stationsStorage.getStationsCollection(type).subscribeOn(scheduler);
    }

    private Observable<StationRecord> syncAndLoadStationsCollection(int type) {
        return syncInitiator.syncRecentStations().flatMap(continueWith(loadStationsCollection(type)));
    }

    ChangeResult saveLastPlayedTrackPosition(Urn collectionUrn, int position) {
        return stationsStorage.saveLastPlayedTrackPosition(collectionUrn, position);
    }

    public Observable<SyncResult> sync() {
        return syncInitiator.syncRecentStations();
    }

    ChangeResult saveRecentlyPlayedStation(Urn stationUrn) {
        final ChangeResult result = stationsStorage.saveUnsyncedRecentlyPlayedStation(stationUrn);
        syncInitiator.requestSystemSync();
        return result;
    }

    public Observable<PlayQueue> fetchUpcomingTracks(final Urn station, final int currentSize) {
        return stationsApi
                .fetchStation(station)
                .doOnNext(storeTracks)
                .doOnNext(storeStationCommand.toAction1())
                .flatMap(loadPlayQueue(station, currentSize))
                .toList()
                .map(toPlayQueue(station))
                .subscribeOn(scheduler);
    }

    public boolean shouldDisplayOnboardingStreamItem() {
        return !stationsStorage.isOnboardingDisabled();
    }

    private Func1<List<StationTrack>, PlayQueue> toPlayQueue(final Urn station) {
        return new Func1<List<StationTrack>, PlayQueue>() {
            @Override
            public PlayQueue call(List<StationTrack> tracks) {
                return PlayQueue.fromStation(station, tracks);
            }
        };
    }

    private Func1<StationRecord, Observable<StationTrack>> loadPlayQueue(final Urn station, final int startPosition) {
        return new Func1<StationRecord, Observable<StationTrack>>() {
            @Override
            public Observable<StationTrack> call(StationRecord ignored) {
                return stationsStorage.loadPlayQueue(station, startPosition);
            }
        };
    }

    public void clearData() {
        stationsStorage.clear();
    }

    public void disableOnboarding() {
        stationsStorage.disableOnboarding();
    }
}
