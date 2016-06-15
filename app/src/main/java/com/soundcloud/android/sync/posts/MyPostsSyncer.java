package com.soundcloud.android.sync.posts;

import com.soundcloud.android.sync.LegacySyncResult;
import com.soundcloud.android.sync.SyncStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;

import javax.inject.Inject;
import javax.inject.Named;

public class MyPostsSyncer implements SyncStrategy {

    private final PostsSyncer trackPostsSyncer;
    private final PostsSyncer playlistPostsSyncer;

    @Inject
    public MyPostsSyncer(@Named(PostsSyncModule.MY_TRACK_POSTS_SYNCER) PostsSyncer trackPostsSyncer,
                         @Named(PostsSyncModule.MY_PLAYLIST_POSTS_SYNCER) PostsSyncer playlistPostsSyncer) {
        this.trackPostsSyncer = trackPostsSyncer;
        this.playlistPostsSyncer = playlistPostsSyncer;
    }

    @NotNull
    @Override
    public LegacySyncResult syncContent(@Deprecated Uri uri, @Nullable String action) throws Exception {
        return trackPostsSyncer.call() || playlistPostsSyncer.call() ?
                LegacySyncResult.fromSuccessfulChange(uri) : LegacySyncResult.fromSuccessWithoutChange(uri);
    }
}
