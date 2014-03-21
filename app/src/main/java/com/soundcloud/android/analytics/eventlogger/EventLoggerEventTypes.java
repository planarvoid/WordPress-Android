package com.soundcloud.android.analytics.eventlogger;

enum EventLoggerEventTypes {
    PLAYBACK("audio"), PLAYBACK_PERFORMANCE("audio_performance");

    private String mPath;
    private EventLoggerEventTypes(String path){
        mPath = path;
    }

    public String getPath() {
        return mPath;
    }
}
