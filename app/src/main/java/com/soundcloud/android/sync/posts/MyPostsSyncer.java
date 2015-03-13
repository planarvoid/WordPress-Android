package com.soundcloud.android.sync.posts;

import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.content.SyncStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;

import javax.inject.Inject;
import javax.inject.Named;

public class MyPostsSyncer implements SyncStrategy {

    private final PostsSyncer trackPostsSyncer;
    private final PostsSyncer playlistPostsSyncer;

    @Inject
    public MyPostsSyncer(@Named("MyTrackPostsSyncer") PostsSyncer trackPostsSyncer,
                         @Named("MyPlaylistPostsSyncer") PostsSyncer playlistPostsSyncer) {
        this.trackPostsSyncer = trackPostsSyncer;
        this.playlistPostsSyncer = playlistPostsSyncer;
    }

    @NotNull
    @Override
    public ApiSyncResult syncContent(@Deprecated Uri uri, @Nullable String action) throws Exception {
        return trackPostsSyncer.call() || playlistPostsSyncer.call() ?
                ApiSyncResult.fromSuccessfulChange(uri) : ApiSyncResult.fromSuccessWithoutChange(uri);
    }
}
