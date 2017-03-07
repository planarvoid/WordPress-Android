package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.analytics.performance.MetricParams;
import com.soundcloud.android.analytics.performance.MetricType;

@AutoValue
public abstract class PerformanceEvent {

    public static PerformanceEvent create(MetricType metricType, MetricParams metricParams) {
        return new AutoValue_PerformanceEvent(metricType, metricParams);
    }

    public abstract MetricType metricType();

    public abstract MetricParams metricParams();
}
