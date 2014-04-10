package com.soundcloud.android.analytics.eventlogger;

import com.google.common.base.Objects;

class EventLoggerEvent {

    private final long timestamp;
    private final String path;
    private final String params;

    EventLoggerEvent(long timestamp, String path, String params) {
        this.timestamp = timestamp;
        this.path = path;
        this.params = params;
    }

    long getTimeStamp(){
        return timestamp;
    }
    String getPath(){
        return path;
    }
    String getParams(){
        return params;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(EventLoggerEvent.class)
                .add("timestamp", getTimeStamp())
                .add("path", getPath())
                .add("params", getParams()).toString();
    }
}
