package com.soundcloud.android.events;

import com.soundcloud.android.utils.annotations.IgnoreHashEquals;
import com.soundcloud.java.optional.Optional;

import java.util.UUID;

public abstract class TrackingEvent {

    protected static String defaultId() {
        return UUID.randomUUID().toString();
    }

    protected static long defaultTimestamp() {
        return System.currentTimeMillis();
    }

    @IgnoreHashEquals public abstract String id();

    @IgnoreHashEquals public abstract long timestamp();

    public abstract Optional<ReferringEvent> referringEvent();

    public long getTimestamp() {
        return timestamp();
    }

    public String getKind() {
        throw new UnsupportedOperationException("Not implemented in new tracking");
    }

    public abstract TrackingEvent putReferringEvent(ReferringEvent referringEvent);
}
