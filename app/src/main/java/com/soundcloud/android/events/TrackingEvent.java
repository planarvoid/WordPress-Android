package com.soundcloud.android.events;

import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.support.v4.util.ArrayMap;

import java.util.Map;
import java.util.UUID;

public class TrackingEvent {

    public static final String KIND_DEFAULT = "default";

    @NotNull protected final String kind;
    @NotNull protected final Map<String, String> attributes;
    protected final long timestamp;
    private final String id;

    protected TrackingEvent(@NotNull String kind, long timestamp, String id) {
        this.kind = kind;
        this.timestamp = timestamp;
        this.attributes = new ArrayMap<>();
        this.id = id;
    }

    protected TrackingEvent(@NotNull String kind, long timestamp) {
        this(kind, timestamp, UUID.randomUUID().toString());
    }

    protected TrackingEvent(@NotNull String kind, String id) {
        this(kind, System.currentTimeMillis(), id);
    }

    protected TrackingEvent(@NotNull String kind) {
        this(kind, System.currentTimeMillis(), UUID.randomUUID().toString());
    }

    @NotNull
    public String getKind() {
        return kind;
    }

    @NotNull
    public String getId() {
        return id;
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

    @Nullable
    public String get(String key) {
        return attributes.get(key);
    }

    public boolean contains(String key) {
        return attributes.containsKey(key);
    }

    public void putReferringEvent(ReferringEvent referringEvent) {
        this.put(ReferringEvent.REFERRING_EVENT_ID_KEY, referringEvent.getId());
        this.put(ReferringEvent.REFERRING_EVENT_KIND_KEY, referringEvent.getKind());
    }

    @NotNull
    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }

        if (o == null || getClass() != o.getClass()) { return false; }

        TrackingEvent that = (TrackingEvent) o;

        if (timestamp != that.timestamp) { return false; }
        if (!kind.equals(that.kind)) { return false; }
        if (!attributes.equals(that.attributes)) { return false; }
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        int result = kind.hashCode();
        result = 31 * result + attributes.hashCode();
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TrackingEvent{" +
                "kind='" + kind + '\'' +
                ", attributes=" + attributes +
                ", timestamp=" + timestamp +
                ", id='" + id + '\'' +
                '}';
    }
}
