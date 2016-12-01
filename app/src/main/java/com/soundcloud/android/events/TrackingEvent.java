package com.soundcloud.android.events;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface TrackingEvent {
    @NotNull
    String getKind();

    @NotNull
    String getId();

    long getTimestamp();

    @Nullable
    String get(String key);

    boolean contains(String key);

    TrackingEvent putReferringEvent(ReferringEvent referringEvent);

    @NotNull
    Map<String, String> getAttributes();
}
