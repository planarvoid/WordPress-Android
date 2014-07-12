package com.soundcloud.android.api.legacy.model;

import com.soundcloud.android.api.legacy.model.behavior.Refreshable;
import org.jetbrains.annotations.NotNull;

public interface UserHolder extends Refreshable {
    @NotNull
    PublicApiUser getUser();
}
