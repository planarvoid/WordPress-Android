package com.soundcloud.android.analytics.performance;

import com.google.auto.value.AutoValue;

/**
 * <p>A `PerformanceMetric` class represents a unique point in time.</p>
 *
 * <p>It consists of these additional properties:</p>
 *
 * - {@link MetricType}<br>
 * - {@link MetricParams}<br>
 *
 * <p>See: {@link PerformanceMetricsEngine}</p>
 */
@AutoValue
public abstract class PerformanceMetric {

    public abstract MetricType metricType();
    public abstract MetricParams metricParams();

    abstract long timestamp();
    abstract TraceMetric traceMetric();

    public static PerformanceMetric create(MetricType type) {
        return builder().metricType(type)
                        .build();
    }

    public static Builder builder() {
        return new AutoValue_PerformanceMetric.Builder()
                .timestamp(System.nanoTime())
                .traceMetric(TraceMetric.EMPTY)
                .metricParams(MetricParams.EMPTY);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder metricType(MetricType metricType);
        public abstract Builder metricParams(MetricParams metricParams);

        abstract Builder timestamp(long timestamp);
        abstract Builder traceMetric(TraceMetric trace);

        abstract MetricType metricType();
        abstract TraceMetric traceMetric();
        abstract PerformanceMetric autoBuild();

        public PerformanceMetric build() {
            MetricType metricType = metricType();
            if (traceMetric().isEmpty()) {
                final TraceMetric traceMetric = TraceMetric.create(metricType);
                traceMetric.start();
                traceMetric(traceMetric);
            }
            return autoBuild();
        }
    }
}
