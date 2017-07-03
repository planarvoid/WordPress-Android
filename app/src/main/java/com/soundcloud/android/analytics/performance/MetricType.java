package com.soundcloud.android.analytics.performance;

/**
 * <p>{@link Enum} that encapsulates the different types of metrics being measured.</p>
 *
 * See: {@link PerformanceMetricsEngine}<br>
 */
public enum MetricType {
    //App Instrumentation Metrics
    DEV_APP_ON_CREATE("dev_app_on_create"),
    DEV_APP_UI_VISIBLE("dev_app_ui_visible"),
    DEV_THREAD_POOL_TASK_WAIT_TIME("dev_thread_pool_task_wait_time"),

    //App Performance Metrics
    TIME_TO_EXPAND_PLAYER("time_to_expand_player"),
    TIME_TO_PLAY("time_to_play"),
    PLAY_QUEUE_LOAD("play_queue_load"),
    TIME_TO_SKIP("time_to_skip"),
    OFFLINE_SYNC("offline_sync"),
    PERFORM_SEARCH("perform_search"),
    COLLECTION_LOAD("collection_load"),
    LISTENING_HISTORY_LOAD("listening_history_load"),
    RECENTLY_PLAYED_LOAD("recently_played_load"),
    LOGIN("login"),
    LOAD_STATION("load_station"),
    PLAY_QUEUE_SHUFFLE("play_queue_shuffle"),
    LIKED_STATIONS_LOAD("liked_stations_load"),
    ACTIVITIES_LOAD("activities_load"),
    SUGGESTED_TRACKS_LOAD("suggested_tracks_load"),
    PLAYLISTS_LOAD("playlists_load"),
    LIKED_TRACKS_FIRST_PAGE_LOAD("liked_tracks_first_page_load"),
    DISCOVERY_LOAD("discovery_load"),
    DISCOVERY_REFRESH("discovery_refresh"),
    STREAM_REFRESH("stream_refresh"),
    STREAM_LOAD("stream_load");

    private final String metricType;

    MetricType(String metricType) {
        this.metricType = metricType;
    }

    @Override
    public String toString() {
        return metricType;
    }
}
