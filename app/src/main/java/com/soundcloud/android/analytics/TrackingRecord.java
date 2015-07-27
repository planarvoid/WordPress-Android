package com.soundcloud.android.analytics;

import com.soundcloud.android.Consts;
import com.soundcloud.java.objects.MoreObjects;

public class TrackingRecord {

    private final long id;
    private final long timestamp;
    private final String backend;
    private final String data;

    public TrackingRecord(long timestamp, String backend, String data) {
        this(Consts.NOT_SET, timestamp, backend, data);
    }

    public TrackingRecord(long id, long timestamp, String backend, String data) {
        this.id = id;
        this.backend = backend;
        this.timestamp = timestamp;
        this.data = data;
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

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(TrackingRecord.class)
                .add("id", id)
                .add("timestamp", timestamp)
                .add("backend", backend)
                .add("data", data).toString();
    }
}
