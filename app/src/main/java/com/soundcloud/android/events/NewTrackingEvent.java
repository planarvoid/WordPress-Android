package com.soundcloud.android.events;

import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public abstract class NewTrackingEvent implements TrackingEvent {

    public enum Kind {
        DEFAULT,
        SEARCH_SUGGESTION,
        SEARCH_LOCAL_SUGGESTION,
        SEARCH_SUBMIT,
        SEARCH_RESULTS,
        SEARCH_FORMULATION_INIT,
        SEARCH_FORMULATION_END
    }

    static String defaultId() {
        return UUID.randomUUID().toString();
    }

    static long defaultTimestamp() {
        return System.currentTimeMillis();
    }

    public abstract Kind kind();

    public abstract String id();

    public abstract long timestamp();

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

    @NotNull
    @Override
    public String getKind() {
        return kind().toString();
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
