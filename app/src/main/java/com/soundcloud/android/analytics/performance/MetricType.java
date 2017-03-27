package com.soundcloud.android.analytics.performance;

/**
 * <p>{@link Enum} that encapsulates the different types of metrics being measured.</p>
 *
 * See: {@link PerformanceMetricsEngine}<br>
 */
public enum MetricType {
    APP_ON_CREATE("app_on_create"),
    APP_UI_VISIBLE("app_ui_visible"),
    TIME_TO_EXPAND_PLAYER("time_to_expand_player"),
    TIME_TO_PLAY("time_to_play"),
    PLAY_QUEUE_LOAD("play_queue_load"),
    TIME_TO_SKIP("time_to_skip"),
    OFFLINE_SYNC("offline_sync"),
    PERFORM_SEARCH("perform_search"),
    COLLECTION_LOAD("collection_load"),
    LISTENING_HISTORY_LOAD("listening_history_load"),
    RECENTLY_PLAYED_LOAD("recently_played_load"),
    LOGIN("login");

    private final String metricType;

    MetricType(String metricType) {
        this.metricType = metricType;
    }

    @Override
    public String toString() {
        return metricType;
    }
}
