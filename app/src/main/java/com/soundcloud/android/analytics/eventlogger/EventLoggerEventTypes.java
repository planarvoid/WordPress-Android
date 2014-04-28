package com.soundcloud.android.analytics.eventlogger;

enum EventLoggerEventTypes {
    PLAYBACK("audio"), PLAYBACK_PERFORMANCE("audio_performance"), PLAYBACK_ERROR("audio_error");

    private final String path;

    private EventLoggerEventTypes(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
