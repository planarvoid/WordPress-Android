package com.soundcloud.android.stations;

import static com.soundcloud.android.playback.DiscoverySource.STATIONS;

import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Context;

import javax.inject.Inject;

public class StartStationHandler {

    private final NavigationExecutor navigationExecutor;
    private final EventBus eventBus;
    private final PerformanceMetricsEngine performanceMetricsEngine;

    @Inject
    public StartStationHandler(NavigationExecutor navigationExecutor, EventBus eventBus, PerformanceMetricsEngine performanceMetricsEngine) {
        this.navigationExecutor = navigationExecutor;
        this.eventBus = eventBus;
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    public void startStation(Context context, Urn stationUrn, DiscoverySource discoverySource) {
        startMeasuringStationLoad();
        navigationExecutor.legacyOpenStationInfo(context, stationUrn, discoverySource);
    }

    public void startStation(Context context, Urn stationUrn) {
        startStation(context, stationUrn, STATIONS);
    }

    public void openStationWithSeedTrack(Context context, Urn seedTrack, UIEvent navigationEvent) {
        startMeasuringStationLoad();
        navigationExecutor.openStationInfo(context,
                                           Urn.forTrackStation(seedTrack.getNumericId()),
                                           seedTrack,
                                           STATIONS,
                                           navigationEvent);
    }

    public void startStationFromPlayer(Context context, Urn trackUrn, boolean trackBlocked) {
        openStationInfo(context, trackUrn, trackBlocked);
    }

    private void openStationInfo(Context context, Urn trackUrn, boolean trackBlocked) {
        eventBus.queue(EventQueue.PLAYER_UI)
                .first(PlayerUIEvent.PLAYER_IS_COLLAPSED)
                .subscribe(new StartStationPageSubscriber(context, trackUrn, trackBlocked));

        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayerAutomatically());
    }

    private void startMeasuringStationLoad() {
        performanceMetricsEngine.startMeasuring(MetricType.LOAD_STATION);
    }

    private class StartStationPageSubscriber extends DefaultSubscriber<PlayerUIEvent> {
        private final Context context;
        private final Urn trackUrn;
        private final boolean trackBlocked;

        StartStationPageSubscriber(Context context, Urn trackUrn, boolean trackBlocked) {
            this.context = context;
            this.trackUrn = trackUrn;
            this.trackBlocked = trackBlocked;
        }

        @Override
        public void onNext(PlayerUIEvent args) {
            final Urn stationUrn = Urn.forTrackStation(trackUrn.getNumericId());

            startMeasuringStationLoad();

            if (trackBlocked) {
                navigationExecutor.legacyOpenStationInfo(context, stationUrn, trackUrn, STATIONS);
            } else {
                navigationExecutor.legacyOpenStationInfo(context, stationUrn, STATIONS);
            }
        }
    }

}
