package com.soundcloud.android.model;

import org.jetbrains.annotations.NotNull;

public interface PlayableHolder extends Creation {
    @NotNull Playable getPlayable();
}
