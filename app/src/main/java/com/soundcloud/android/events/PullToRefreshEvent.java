package com.soundcloud.android.events;

import com.soundcloud.android.main.Screen;
import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.Metric;

public class PullToRefreshEvent extends TrackingEvent implements MetricEvent {

    private final Screen screen;

    public PullToRefreshEvent(Screen screen) {
        super(TrackingEvent.KIND_DEFAULT, System.currentTimeMillis());
        this.screen = screen;
    }

    @Override
    public Metric toMetric() {
        return Metric.create("PullToRefresh", DataPoint.string("screen", screen.get()));
    }
}
