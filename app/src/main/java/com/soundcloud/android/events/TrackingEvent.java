package com.soundcloud.android.events;

import org.jetbrains.annotations.Nullable;

import android.support.v4.util.ArrayMap;

import java.util.Map;

public abstract class TrackingEvent {

    public static final String KIND_DEFAULT = "default";

    protected String kind = KIND_DEFAULT;
    protected final long timeStamp;
    protected final Map<String, String> attributes;

    protected TrackingEvent(String kind, long timeStamp) {
        this.kind = kind;
        this.timeStamp = timeStamp;
        this.attributes = new ArrayMap<>();
    }

    public String getKind() {
        return kind;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public TrackingEvent put(String key, @Nullable String value) {
        attributes.put(key, value);
        return this;
    }

    public String get(String key) {
        return attributes.get(key);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
}
