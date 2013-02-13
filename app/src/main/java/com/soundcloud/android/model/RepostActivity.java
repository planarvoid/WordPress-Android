package com.soundcloud.android.model;

import org.jetbrains.annotations.NotNull;

public interface RepostActivity extends Creation {
    @NotNull User getReposter();
}
