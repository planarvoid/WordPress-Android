package com.soundcloud.android.events;

import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.support.v4.util.ArrayMap;

import java.util.Map;

public class TrackingEvent {

    public static final String KIND_DEFAULT = "default";

    @NotNull protected final String kind;
    @NotNull protected final Map<String, String> attributes;
    protected final long timestamp;

    protected TrackingEvent(@NotNull String kind, long timestamp) {
        this.kind = kind;
        this.timestamp = timestamp;
        this.attributes = new ArrayMap<>();
    }

    @NotNull
    public String getKind() {
        return kind;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public <T extends TrackingEvent> T put(String key, @Nullable String value) {
        attributes.put(key, value);
        return (T) this;
    }

    public <T extends TrackingEvent> T put(String key, Optional<?> value) {
        if (value.isPresent()) {
            attributes.put(key, value.get().toString());
        }
        return (T) this;
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

        if (timestamp != event.timestamp) {
            return false;
        }
        if (!attributes.equals(event.attributes)) {
            return false;
        }
        return kind.equals(event.kind);

    }

    @Override
    public final int hashCode() {
        int result = kind.hashCode();
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + attributes.hashCode();
        return result;
    }
}
