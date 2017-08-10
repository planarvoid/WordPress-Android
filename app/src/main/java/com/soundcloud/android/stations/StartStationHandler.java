package com.soundcloud.android.stations;

import static com.soundcloud.android.playback.DiscoverySource.STATIONS;

import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.java.optional.Optional;

import javax.inject.Inject;

public class StartStationHandler {

    private final Navigator navigatior;
    private final PerformanceMetricsEngine performanceMetricsEngine;

    @Inject
    public StartStationHandler(Navigator navigatior, PerformanceMetricsEngine performanceMetricsEngine) {
        this.navigatior = navigatior;
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    public void startStation(Urn stationUrn, DiscoverySource discoverySource) {
        startMeasuringStationLoad();
        navigatior.navigateTo(NavigationTarget.forStationInfo(stationUrn, Optional.absent(), Optional.of(discoverySource), Optional.absent()));
    }

    public void startStation(Urn stationUrn) {
        startStation(stationUrn, STATIONS);
    }

    public void openStationWithSeedTrack(Urn seedTrack, UIEvent navigationEvent) {
        startMeasuringStationLoad();
        navigatior.navigateTo(NavigationTarget.forStationInfo(Urn.forTrackStation(seedTrack.getNumericId()), Optional.of(seedTrack), Optional.of(STATIONS), Optional.of(navigationEvent)));
    }

    private void startMeasuringStationLoad() {
        performanceMetricsEngine.startMeasuring(MetricType.LOAD_STATION);
    }
}
