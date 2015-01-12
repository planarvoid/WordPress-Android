package com.soundcloud.android.sync.content;

import com.soundcloud.android.sync.ApiSyncResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;

public interface SyncStrategy {
    @NotNull
    ApiSyncResult syncContent(@Deprecated Uri uri, @Nullable String action) throws Exception;
}
