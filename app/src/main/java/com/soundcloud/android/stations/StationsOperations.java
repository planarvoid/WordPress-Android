package com.soundcloud.android.stations;

import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.propeller.ChangeResult;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;

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

    private Observable<Station> fetchStation(Urn stationUrn) {
        return stationsApi
                .fetchStation(stationUrn)
                .doOnNext(storeTracks)
                .doOnNext(storeStationCommand.toAction())
                .map(TO_STATION);
    }

    Observable<ChangeResult> saveLastPlayedTrackPosition(Urn collectionUrn, int position) {
        return stationsStorage
                .saveLastPlayedTrackPosition(collectionUrn, position)
                .subscribeOn(scheduler);
    }

    Observable<Station> collection(final int type) {
        return loadStationsCollection(type)
                .switchIfEmpty(syncAndReloadStations(type))
                .subscribeOn(scheduler);
    }

    Observable<SyncResult> sync() {
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
}
