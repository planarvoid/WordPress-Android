package com.soundcloud.android.analytics.performance;

/**
 * <p>{@link Enum} that encapsulates the different types of metrics being measured.</p>
 *
 * See: {@link PerformanceMetricsEngine}<br>
 */
public enum MetricType {
    APP_ON_CREATE("app_on_create"),
    APP_UI_VISIBLE("app_ui_visible"),
    EXTENDED_TIME_TO_PLAY("extended_time_to_play");

    private final String metricType;

    MetricType(String metricType) {
        this.metricType = metricType;
    }

    @Override
    public String toString() {
        return metricType;
    }
}
