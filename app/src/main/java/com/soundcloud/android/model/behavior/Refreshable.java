package com.soundcloud.android.model.behavior;

import org.jetbrains.annotations.Nullable;

public interface Refreshable {
    @Nullable
    Refreshable getRefreshableResource();
    boolean isStale();
    boolean isIncomplete();
}
