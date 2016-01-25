package com.soundcloud.android.events;

import com.soundcloud.reporting.Metric;

public interface MetricEvent {

    Metric toMetric();

}
