package com.soundcloud.android.sync.likes;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.LegacySyncJob;
import dagger.Lazy;

import javax.inject.Inject;
import javax.inject.Named;

public class SyncPlaylistLikesJob extends DefaultSyncJob {

    @Inject
    public SyncPlaylistLikesJob(@Named("PlaylistLikesSyncer") Lazy<LikesSyncer<ApiPlaylist>> playlistLikesSyncer) {
        super(playlistLikesSyncer.get());
    }

    @Override
    public boolean equals(Object job) {
        return job instanceof SyncPlaylistLikesJob || isLegacyMyLikesSyncJob(job);
    }

    private boolean isLegacyMyLikesSyncJob(Object job) {
        return (job instanceof LegacySyncJob && ((LegacySyncJob) job).getContentUri().equals(Content.ME_LIKES.uri));
    }

}
