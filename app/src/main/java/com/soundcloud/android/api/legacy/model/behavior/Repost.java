package com.soundcloud.android.api.legacy.model.behavior;

import com.soundcloud.android.api.legacy.model.PublicApiUser;
import org.jetbrains.annotations.NotNull;

public interface Repost {
    @NotNull
    PublicApiUser getReposter();
}
