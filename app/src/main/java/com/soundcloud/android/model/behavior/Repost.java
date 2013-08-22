package com.soundcloud.android.model.behavior;

import com.soundcloud.android.model.User;
import org.jetbrains.annotations.NotNull;

public interface Repost extends Creation {
    @NotNull
    User getReposter();
}
