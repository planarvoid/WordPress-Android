package com.soundcloud.android.analytics.performance;

/**
 * <p>{@link Enum} that encapsulates the different keys used in {@link MetricParams}.</p>
 *
 * See: {@link PerformanceMetricsEngine}<br>
 */
public enum MetricKey {
    USER_LOGGED_IN("logged_in_user"),
    TIME_MILLIS("time_in_millis"),
    PLAY_QUEUE_SIZE("play_queue_size"),
    SCREEN("screen");

    private final String metricKey;

    MetricKey(String metricKey) {
        this.metricKey = metricKey;
    }

    @Override
    public String toString() {
        return metricKey;
    }
}
