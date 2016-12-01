package com.soundcloud.android.events;

import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.Metric;

public class BackgroundSyncEvent extends LegacyTrackingEvent implements MetricEvent {

    private final int syncableCount;

    public BackgroundSyncEvent(int syncableCount) {
        super("BackgroundSyncEvent", syncableCount);
        this.syncableCount = syncableCount;
    }

    @Override
    public Metric toMetric() {
        return Metric.create("BackgroundSync", DataPoint.numeric("syncableCount", syncableCount));
    }
}
