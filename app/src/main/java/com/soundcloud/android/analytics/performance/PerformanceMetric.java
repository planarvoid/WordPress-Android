package com.soundcloud.android.analytics.performance;

import com.google.auto.value.AutoValue;

/**
 * <p>A `PerformanceMetric` class represents a unique point in time.</p>
 *
 * <p>It consists of these additional properties:</p>
 *
 * - {@link MetricType}<br>
 * - Timestamp<br>
 * - {@link MetricParams}<br>
 *
 * <p>See: {@link PerformanceMetricsEngine}</p>
 */
@AutoValue
public abstract class PerformanceMetric {

    public abstract MetricType metricType();

    public abstract long timestamp();

    public abstract MetricParams metricParams();

    public static PerformanceMetric create(MetricType type) {
        return builder().metricType(type)
                        .build();
    }

    public static Builder builder() {
        return new AutoValue_PerformanceMetric.Builder()
                .timestamp(System.nanoTime())
                .metricParams(MetricParams.EMPTY);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder metricType(MetricType metricType);

        public abstract Builder timestamp(long timestamp);

        public abstract Builder metricParams(MetricParams metricParams);

        public abstract PerformanceMetric build();
    }
}
