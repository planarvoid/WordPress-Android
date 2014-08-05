package com.soundcloud.android.analytics;

import com.google.common.base.Objects;
import com.soundcloud.android.Consts;

public class TrackingEvent {

    private final long id;
    private final long timestamp;
    private final String backend;
    private final String url;

    public TrackingEvent(long timestamp, String backend, String url) {
        this(Consts.NOT_SET, timestamp, backend, url);
    }

    public TrackingEvent(long id, long timestamp, String backend, String url) {
        this.id = id;
        this.backend = backend;
        this.timestamp = timestamp;
        this.url = url;
    }

    public long getId() {
        return id;
    }

    public long getTimeStamp() {
        return timestamp;
    }

    public String getBackend() {
        return backend;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(TrackingEvent.class)
                .add("id", id)
                .add("timestamp", timestamp)
                .add("backend", backend)
                .add("url", url).toString();
    }
}
