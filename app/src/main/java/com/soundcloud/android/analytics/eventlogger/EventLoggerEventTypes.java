package com.soundcloud.android.analytics.eventlogger;

enum EventLoggerEventTypes {
    PLAYBACK("audio"), PLAYBACK_PERFORMANCE("audio_performance"), PLAYBACK_ERROR("audio_error");

    private String mPath;
    private EventLoggerEventTypes(String path){
        mPath = path;
    }

    public String getPath() {
        return mPath;
    }
}
