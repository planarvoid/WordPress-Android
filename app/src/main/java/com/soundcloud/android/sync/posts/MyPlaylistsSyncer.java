package com.soundcloud.android.sync.posts;

import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.content.SyncStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;

import javax.inject.Inject;
import javax.inject.Named;

public class MyPlaylistsSyncer implements SyncStrategy {

    private final PostsSyncer postsSyncer;

    @Inject
    public MyPlaylistsSyncer(@Named("MyPlaylistPostsSyncer") PostsSyncer postsSyncer) {
        this.postsSyncer = postsSyncer;
    }

    @NotNull
    @Override
    public ApiSyncResult syncContent(@Deprecated Uri uri, @Nullable String action) throws Exception {
        return postsSyncer.call()
                ? ApiSyncResult.fromSuccessfulChange(uri)
                : ApiSyncResult.fromSuccessWithoutChange(uri);
    }
}
