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
    SCREEN("screen"),
    SKIP_ORIGIN("skip_origin"),
    DOWNLOADED_DURATION("downloaded_duration_ms"),
    LISTENING_HISTORY_SIZE("listening_history_size"),
    RECENTLY_PLAYED_SIZE("recently_played_size"),
    LOGIN_PROVIDER("login_provider"),
    HOME_SCREEN("home_screen"),
    TRACKS_COUNT("tracks_count");

    private final String metricKey;

    MetricKey(String metricKey) {
        this.metricKey = metricKey;
    }

    @Override
    public String toString() {
        return metricKey;
    }
}
