package com.soundcloud.android.stations;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.model.Urn;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

class StationTrackOperations {

    private StationsStorage stationsStorage;
    private final Scheduler scheduler;

    @Inject
    StationTrackOperations(StationsStorage stationsStorage,
                           @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.stationsStorage = stationsStorage;
        this.scheduler = scheduler;
    }

    Observable<List<StationInfoTrack>> initialStationTracks(Urn stationUrn) {
        return stationsStorage.stationTracks(stationUrn).toList()
                              .subscribeOn(scheduler);
    }
}
