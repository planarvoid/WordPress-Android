package com.soundcloud.android.analytics.performance;

import android.os.Bundle;

/**
 * <p>Wrapper backed by a key/value map used to send parameters in {@link PerformanceMetric}.</p>
 *
 * See: {@link PerformanceMetricsEngine}<br>
 */
public class MetricParams {

    public static MetricParams EMPTY = new MetricParams();

    private final Bundle bundle = new Bundle();

    public MetricParams putBoolean(MetricKey key, boolean value) {
        bundle.putBoolean(key.toString(), value);
        return this;
    }

    public MetricParams putLong(MetricKey key, long value) {
        bundle.putLong(key.toString(), value);
        return this;
    }

    void putAll(MetricParams metricParams) {
        bundle.putAll(metricParams.bundle);
    }

    Bundle toBundle() {
        return bundle;
    }
}
