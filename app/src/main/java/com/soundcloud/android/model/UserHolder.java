package com.soundcloud.android.model;

import com.soundcloud.android.model.behavior.Refreshable;
import org.jetbrains.annotations.NotNull;

public interface UserHolder extends Refreshable {
    @NotNull User getUser();
}
