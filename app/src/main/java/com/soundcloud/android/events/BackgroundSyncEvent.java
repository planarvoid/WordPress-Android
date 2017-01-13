package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.Metric;

@AutoValue
public abstract class BackgroundSyncEvent extends NewTrackingEvent implements MetricEvent {

    abstract int syncableCount();

    public static BackgroundSyncEvent create(int syncableCount) {
        return new AutoValue_BackgroundSyncEvent(defaultId(), defaultTimestamp(), Optional.absent(), syncableCount);
    }

    @Override
    public BackgroundSyncEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_BackgroundSyncEvent(getId(), getTimestamp(), Optional.of(referringEvent), syncableCount());
    }

    @Override
    public Metric toMetric() {
        return Metric.create("BackgroundSync", DataPoint.numeric("syncableCount", syncableCount()));
    }
}
