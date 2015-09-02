package com.soundcloud.android.stations;

import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.java.functions.Function;
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
            storeTracksCommand.call(apiStation.getTracks());
        }
    };

    private static final Func1<ApiStation, Station> toStation = new Func1<ApiStation, Station>() {
        private static final int START_POSITION = 0;

        private final Function<TrackRecord, Urn> toUrns = new Function<TrackRecord, Urn>() {
            @Override
            public Urn apply(TrackRecord track) {
                return track.getUrn();
            }
        };

        @Override
        public Station call(ApiStation station) {
            return new Station(
                    station.getUrn(),
                    station.getTitle(),
                    station.getType(),
                    transform(station.getTracks().getCollection(), toUrns),
                    START_POSITION
            );
        }
    };

    private final Func1<SyncResult, Observable<Station>> toRecentStations = new Func1<SyncResult, Observable<Station>>() {
        @Override
        public Observable<Station> call(SyncResult ignored) {
            return recentStations();
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
                        stationsStorage.station(stationUrn),
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
                .map(toStation);
    }

    Observable<ChangeResult> saveLastPlayedTrackPosition(Urn collectionUrn, int position) {
        return stationsStorage
                .saveLastPlayedTrackPosition(collectionUrn, position)
                .subscribeOn(scheduler);
    }

    Observable<Station> recentStations() {
        return stationsStorage
                .recentStations()
                .subscribeOn(scheduler);
    }

    Observable<Station> updateRecentStations() {
        return syncInitiator
                .syncRecentStations()
                .flatMap(toRecentStations);
    }

    public void clearData() {
        stationsStorage.clear();
    }

}
