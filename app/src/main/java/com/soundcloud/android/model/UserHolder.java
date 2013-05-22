package com.soundcloud.android.model;

import org.jetbrains.annotations.NotNull;

public interface UserHolder extends Refreshable {
    @NotNull User getUser();
}
