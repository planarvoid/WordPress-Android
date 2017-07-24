package com.soundcloud.android.stations;

import static com.soundcloud.android.playback.DiscoverySource.STATIONS;

import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.IntentFactory;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.java.optional.Optional;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class StartStationHandler {

    private final Navigator navigatior;
    private final PerformanceMetricsEngine performanceMetricsEngine;

    @Inject
    public StartStationHandler(Navigator navigatior, PerformanceMetricsEngine performanceMetricsEngine) {
        this.navigatior = navigatior;
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    public Intent getStartStationIntent(Context context, Urn stationUrn, DiscoverySource discoverySource) {
        startMeasuringStationLoad();
        return IntentFactory.createStationsInfoIntent(context, stationUrn, Optional.absent(), Optional.of(discoverySource));
    }

    public void startStation(Activity activity, Urn stationUrn, DiscoverySource discoverySource) {
        startMeasuringStationLoad();
        navigatior.navigateTo(activity, NavigationTarget.forStationInfo(stationUrn, Optional.absent(), Optional.of(discoverySource), Optional.absent()));
    }

    public void startStation(Activity activity, Urn stationUrn) {
        startStation(activity, stationUrn, STATIONS);
    }

    public void openStationWithSeedTrack(Activity activity, Urn seedTrack, UIEvent navigationEvent) {
        startMeasuringStationLoad();
        navigatior.navigateTo(activity, NavigationTarget.forStationInfo(Urn.forTrackStation(seedTrack.getNumericId()), Optional.of(seedTrack), Optional.of(STATIONS), Optional.of(navigationEvent)));
    }

    private void startMeasuringStationLoad() {
        performanceMetricsEngine.startMeasuring(MetricType.LOAD_STATION);
    }
}
