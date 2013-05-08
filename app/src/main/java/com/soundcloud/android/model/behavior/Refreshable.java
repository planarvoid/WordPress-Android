package com.soundcloud.android.model.behavior;

import com.soundcloud.android.model.ScResource;
import org.jetbrains.annotations.Nullable;

public interface Refreshable {
    @Nullable
    ScResource getRefreshableResource();
    boolean isStale();
    boolean isIncomplete();
}
