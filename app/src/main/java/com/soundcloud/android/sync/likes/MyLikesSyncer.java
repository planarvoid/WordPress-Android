package com.soundcloud.android.sync.likes;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.content.SyncStrategy;
import dagger.Lazy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;

import javax.inject.Inject;
import javax.inject.Named;

public class MyLikesSyncer implements SyncStrategy {

    private final Lazy<LikesSyncer<ApiTrack>> trackLikesSyncer;
    private final Lazy<LikesSyncer<ApiPlaylist>> playlistLikesSyncer;

    @Inject
    public MyLikesSyncer(@Named(LikesSyncModule.TRACK_LIKES_SYNCER) Lazy<LikesSyncer<ApiTrack>> trackLikesSyncer,
                         @Named(LikesSyncModule.PLAYLIST_LIKES_SYNCER) Lazy<LikesSyncer<ApiPlaylist>> playlistLikesSyncer) {
        this.trackLikesSyncer = trackLikesSyncer;
        this.playlistLikesSyncer = playlistLikesSyncer;
    }

    @NotNull
    @Override
    public ApiSyncResult syncContent(@Deprecated Uri uri, @Nullable String action) throws Exception {
        Boolean tracksChanged = trackLikesSyncer.get().call();
        Boolean playlistsChanged = playlistLikesSyncer.get().call();
        return tracksChanged || playlistsChanged
                ? ApiSyncResult.fromSuccessfulChange(uri) : ApiSyncResult.fromSuccessWithoutChange(uri);
    }
}
