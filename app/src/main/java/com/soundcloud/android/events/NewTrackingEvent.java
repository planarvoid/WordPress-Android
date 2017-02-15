package com.soundcloud.android.events;

import com.soundcloud.android.utils.annotations.IgnoreHashEquals;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public abstract class NewTrackingEvent implements TrackingEvent {

    protected static String defaultId() {
        return UUID.randomUUID().toString();
    }

    protected static long defaultTimestamp() {
        return System.currentTimeMillis();
    }

    @IgnoreHashEquals public abstract String id();

    @IgnoreHashEquals public abstract long timestamp();

    public abstract Optional<ReferringEvent> referringEvent();

    @Override
    public long getTimestamp() {
        return timestamp();
    }

    @NotNull
    @Override
    public String getId() {
        return id();
    }

    @Override
    public String getKind() {
        throw new UnsupportedOperationException("Not implemented in new tracking");
    }

    @Nullable
    @Override
    public String get(String key) {
        throw new UnsupportedOperationException("Not implemented in new tracking");
    }

    @Override
    public boolean contains(String key) {
        throw new UnsupportedOperationException("Not implemented in new tracking");
    }

    @NotNull
    @Override
    public Map<String, String> getAttributes() {
        throw new UnsupportedOperationException("Not implemented in new tracking");
    }
}
