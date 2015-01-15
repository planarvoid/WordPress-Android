package com.soundcloud.android.sync.likes;

import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.content.SyncStrategy;
import dagger.Lazy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;

import javax.inject.Inject;
import javax.inject.Named;

public class MyLikesSyncer implements SyncStrategy {

    private final Lazy<LikesSyncer> trackLikesSyncer;
    private final Lazy<LikesSyncer> playlistLikesSyncer;

    @Inject
    public MyLikesSyncer(@Named("TrackLikesSyncer") Lazy<LikesSyncer> trackLikesSyncer,
                         @Named("PlaylistLikesSyncer") Lazy<LikesSyncer> playlistLikesSyncer) {
        this.trackLikesSyncer = trackLikesSyncer;
        this.playlistLikesSyncer = playlistLikesSyncer;
    }

    @NotNull
    @Override
    public ApiSyncResult syncContent(@Deprecated Uri uri, @Nullable String action) throws Exception {
        return trackLikesSyncer.get().syncContent() || playlistLikesSyncer.get().syncContent()
                ? ApiSyncResult.fromSuccessfulChange(uri) : ApiSyncResult.fromSuccessWithoutChange(uri);
    }
}
