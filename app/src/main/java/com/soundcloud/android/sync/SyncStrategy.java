package com.soundcloud.android.sync;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;

public interface SyncStrategy {
    @NotNull
    LegacySyncResult syncContent(@Deprecated Uri uri, @Nullable String action) throws Exception;
}
