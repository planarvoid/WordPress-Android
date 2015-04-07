package com.soundcloud.android.sync.likes;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.LegacySyncJob;
import dagger.Lazy;

import javax.inject.Inject;
import javax.inject.Named;

public class SyncTrackLikesJob extends DefaultSyncJob {

    @Inject
    public SyncTrackLikesJob(@Named(LikesSyncModule.TRACK_LIKES_SYNCER) Lazy<LikesSyncer<ApiTrack>> trackLikesSyncer) {
        super(trackLikesSyncer.get());
    }

    @Override
    public boolean equals(Object job) {
        return job instanceof SyncTrackLikesJob || isLegacyMyLikesSyncJob(job);
    }

    private boolean isLegacyMyLikesSyncJob(Object job) {
        return (job instanceof LegacySyncJob && ((LegacySyncJob) job).getContentUri().equals(Content.ME_LIKES.uri));
    }

}
