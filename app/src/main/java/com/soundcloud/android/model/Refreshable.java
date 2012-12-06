package com.soundcloud.android.model;

import org.jetbrains.annotations.Nullable;

public interface Refreshable {
    @Nullable ScResource getRefreshableResource();
    boolean isStale();
}
