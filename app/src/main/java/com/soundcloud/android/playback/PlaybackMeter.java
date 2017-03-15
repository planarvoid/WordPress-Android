package com.soundcloud.android.playback;

import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;

public class PlaybackMeter {

    private final EventBus eventBus;
    private final PerformanceMetricsEngine engine;

    @Inject
    PlaybackMeter(EventBus eventBus, PerformanceMetricsEngine engine) {
        this.eventBus = eventBus;
        this.engine = engine;
    }

    public void subscribe() {
        eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED)
                .filter(PlayStateEvent::isPlayerPlaying)
                .subscribe(event -> {
                    engine.endMeasuring(MetricType.TIME_TO_PLAY);
                    engine.endMeasuring(MetricType.TIME_TO_SKIP);
                });
    }

}
