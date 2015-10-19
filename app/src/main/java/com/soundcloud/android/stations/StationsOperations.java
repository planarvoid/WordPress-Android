package com.soundcloud.android.stations;

import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.model.StationRecord;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.propeller.ChangeResult;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.internal.util.UtilityFunctions;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class StationsOperations {
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
    public StationsOperations(StationsStorage stationsStorage,
                              StationsApi stationsApi,
                              StoreTracksCommand storeTracksCommand,
                              StoreStationCommand storeStationCommand,
                              StationsSyncInitiator syncInitiator,
                              @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
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

    public Observable<StationRecord> stationWithSeed(Urn station, final Urn seed) {
        return station(station, prependSeed(seed));
    }

    private Observable<StationRecord> station(Urn station, Func1<StationRecord, StationRecord> toStation) {
        return Observable
                .concat(
                        stationsStorage.station(station).filter(HAS_TRACKS),
                        fetchStation(station)
                                .map(toStation)
                                .doOnNext(storeStationCommand.toAction())
                )
                .first()
                .subscribeOn(scheduler);
    }

    private Func1<StationRecord, StationRecord> prependSeed(final Urn seed) {
        return new Func1<StationRecord, StationRecord>() {
            @Override
            public StationRecord call(StationRecord station) {
                return Station.stationWithSeedTrack(station, seed);
            }
        };
    }

    public Observable<ChangeResult> saveRecentlyPlayedStation(Urn stationUrn) {
        return stationsStorage
                .saveUnsyncedRecentlyPlayedStation(stationUrn)
                .doOnCompleted(syncInitiator.requestSystemSyncAction())
                .subscribeOn(scheduler);
    }

    public Observable<PlayQueue> fetchUpcomingTracks(final Urn station, final int currentSize) {
        return stationsApi
                .fetchStation(station)
                .doOnNext(storeTracks)
                .doOnNext(storeStationCommand.toAction())
                .flatMap(toTracks(station, currentSize))
                .toList()
                .map(toPlayQueue(station))
                .subscribeOn(scheduler);
    }

    public boolean shouldDisplayOnboardingStreamItem() {
        return !stationsStorage.isOnboardingDisabled();
    }

    @NonNull
    private Func1<List<Urn>, PlayQueue> toPlayQueue(final Urn station) {
        return new Func1<List<Urn>, PlayQueue>() {
            @Override
            public PlayQueue call(List<Urn> tracks) {
                return PlayQueue.fromStation(station, tracks);
            }
        };
    }

    @NonNull
    private Func1<ApiStation, Observable<Urn>> toTracks(final Urn station, final int startPosition) {
        return new Func1<ApiStation, Observable<Urn>>() {
            @Override
            public Observable<Urn> call(ApiStation apiStation) {
                return stationsStorage.loadPlayQueue(station, startPosition);
            }
        };
    }

    private Observable<? extends StationRecord> fetchStation(Urn stationUrn) {
        return stationsApi
                .fetchStation(stationUrn)
                .doOnNext(storeTracks);
    }

    Observable<ChangeResult> saveLastPlayedTrackPosition(Urn collectionUrn, int position) {
        return stationsStorage
                .saveLastPlayedTrackPosition(collectionUrn, position)
                .subscribeOn(scheduler);
    }

    public Observable<StationRecord> collection(final int type) {
        return loadStationsCollection(type)
                .switchIfEmpty(syncAndReloadStations(type))
                .subscribeOn(scheduler);
    }

    public Observable<SyncResult> sync() {
        return syncInitiator.syncRecentStations();
    }

    private Observable<StationRecord> loadStationsCollection(final int type) {
        return stationsStorage.getStationsCollection(type).subscribeOn(scheduler);
    }

    private Observable<StationRecord> syncAndReloadStations(final int type) {
        return syncInitiator.syncRecentStations().flatMap(continueWith(loadStationsCollection(type)));
    }

    public void clearData() {
        stationsStorage.clear();
    }

    public void disableOnboarding() {
        stationsStorage.disableOnboarding();
    }
}
