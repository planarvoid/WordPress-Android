package com.soundcloud.android.stations;

import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.model.ApiStation;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.java.functions.Function;
import com.soundcloud.propeller.ChangeResult;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StationsOperations {
    private final StationsStorage stationsStorage;
    private final StationsApi stationsApi;
    private final StoreTracksCommand storeTracksCommand;
    private final StoreStationCommand storeStationCommand;
    private final Scheduler scheduler;

    private final Action1<ApiStation> storeTracks = new Action1<ApiStation>() {
        @Override
        public void call(ApiStation apiStation) {
            storeTracksCommand.call(apiStation.getTracks());
        }
    };

    private static final Func1<ApiStation, Station> toStation = new Func1<ApiStation, Station>() {

        private final Function<TrackRecord, Urn> toUrns = new Function<TrackRecord, Urn>() {
            @Override
            public Urn apply(TrackRecord track) {
                return track.getUrn();
            }
        };

        @Override
        public Station call(ApiStation station) {
            return new Station(station.getInfo().getUrn(), station.getInfo().getTitle(), transform(station.getTracks().getCollection(), toUrns), 0);
        }
    };

    @Inject
    public StationsOperations(StationsStorage stationsStorage,
                              StationsApi stationsApi,
                              StoreTracksCommand storeTracksCommand,
                              StoreStationCommand storeStationCommand,
                              @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.stationsStorage = stationsStorage;
        this.stationsApi = stationsApi;
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
                .saveRecentlyPlayedStation(stationUrn)
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

    public Observable<List<Station>> recentStations() {
        return stationsStorage
                .recentStations()
                .subscribeOn(scheduler);
    }

    @NonNull
    private Station getTestStation() {
        return new Station(Urn.forTrackStation(123L), "test title", Collections.<Urn>emptyList(), 0);
    }
}
