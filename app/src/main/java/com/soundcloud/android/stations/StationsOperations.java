package com.soundcloud.android.stations;

import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.propeller.ChangeResult;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class StationsOperations {
    private final StationsStorage stationsStorage;
    private final StationsApi stationsApi;
    private final StoreTracksCommand storeTracksCommand;
    private final StoreApiStationCommand storeApiStationCommand;
    private final StationsSyncInitiator syncInitiator;
    private final Scheduler scheduler;

    private final Action1<ApiStation> storeTracks = new Action1<ApiStation>() {
        @Override
        public void call(ApiStation apiStation) {
            storeTracksCommand.call(apiStation.getTrackRecords());
        }
    };

    private static final Func1<Station, Boolean> HAS_TRACKS = new Func1<Station, Boolean>() {
        @Override
        public Boolean call(Station station) {
            return station.getTracks().size() > 0;
        }
    };

    private static final Func1<ApiStation, Station> TO_STATION = new Func1<ApiStation, Station>() {

        @Override
        public Station call(ApiStation station) {
            return new Station(
                    station.getUrn(),
                    station.getTitle(),
                    station.getType(),
                    station.getTracks(),
                    station.getPermalink(),
                    Consts.NOT_SET
            );
        }
    };

    @Inject
    public StationsOperations(StationsStorage stationsStorage,
                              StationsApi stationsApi,
                              StoreTracksCommand storeTracksCommand,
                              StoreApiStationCommand storeApiStationCommand,
                              StationsSyncInitiator syncInitiator,
                              @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.stationsStorage = stationsStorage;
        this.stationsApi = stationsApi;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
        this.storeTracksCommand = storeTracksCommand;
        this.storeApiStationCommand = storeApiStationCommand;
    }

    public Observable<Station> station(Urn stationUrn) {
        return Observable
                .concat(
                        stationsStorage.station(stationUrn).filter(HAS_TRACKS),
                        fetchStation(stationUrn)
                )
                .first()
                .subscribeOn(scheduler);
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
                .doOnNext(storeApiStationCommand.toAction())
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

    private Observable<Station> fetchStation(Urn stationUrn) {
        return stationsApi
                .fetchStation(stationUrn)
                .doOnNext(storeTracks)
                .doOnNext(storeApiStationCommand.toAction())
                .map(TO_STATION);
    }

    Observable<ChangeResult> saveLastPlayedTrackPosition(Urn collectionUrn, int position) {
        return stationsStorage
                .saveLastPlayedTrackPosition(collectionUrn, position)
                .subscribeOn(scheduler);
    }

    public Observable<Station> collection(final int type) {
        return loadStationsCollection(type)
                .switchIfEmpty(syncAndReloadStations(type))
                .subscribeOn(scheduler);
    }

    public Observable<SyncResult> sync() {
        return syncInitiator.syncRecentStations();
    }

    private Observable<Station> loadStationsCollection(final int type) {
        return stationsStorage.getStationsCollection(type).subscribeOn(scheduler);
    }

    private Observable<Station> syncAndReloadStations(final int type) {
        return syncInitiator.syncRecentStations().flatMap(continueWith(loadStationsCollection(type)));
    }

    public void clearData() {
        stationsStorage.clear();
    }

    void disableOnboarding() {
        stationsStorage.disableOnboarding();
    }
}
