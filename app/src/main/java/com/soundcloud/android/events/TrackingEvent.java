package com.soundcloud.android.events;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.support.v4.util.ArrayMap;

import java.util.Map;

public class TrackingEvent {

    public static final String KIND_DEFAULT = "default";

    @NotNull protected final String kind;
    @NotNull protected final Map<String, String> attributes;
    protected final long timeStamp;

    protected TrackingEvent(@NotNull String kind, long timeStamp) {
        this.kind = kind;
        this.timeStamp = timeStamp;
        this.attributes = new ArrayMap<>();
    }

    @NotNull
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

    @NotNull
    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || o.getClass() != getClass()) {
            return false;
        }

        TrackingEvent event = (TrackingEvent) o;

        if (timeStamp != event.timeStamp) {
            return false;
        }
        if (!attributes.equals(event.attributes)) {
            return false;
        }
        if (!kind.equals(event.kind)) {
            return false;
        }

        return true;
    }

    @Override
    public final int hashCode() {
        int result = kind.hashCode();
        result = 31 * result + (int) (timeStamp ^ (timeStamp >>> 32));
        result = 31 * result + attributes.hashCode();
        return result;
    }
}
