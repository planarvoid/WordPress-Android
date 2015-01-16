package com.soundcloud.android.sync;

import com.soundcloud.android.sync.likes.SyncPlaylistLikesJob;
import com.soundcloud.android.sync.likes.SyncTrackLikesJob;
import com.soundcloud.android.sync.likes.SingleJobRequest;
import dagger.Lazy;

import android.content.Intent;

import javax.inject.Inject;

public class SyncRequestFactory {

    private final LegacySyncRequest.Factory syncIntentFactory;
    private final Lazy<SyncTrackLikesJob> lazySyncTrackLikesJob;
    private final Lazy<SyncPlaylistLikesJob> lazySyncPlaylistLikesJob;

    @Inject
    public SyncRequestFactory(LegacySyncRequest.Factory syncIntentFactory,
                              Lazy<SyncTrackLikesJob> lazySyncTrackLikesJob,
                              Lazy<SyncPlaylistLikesJob> lazySyncPlaylistLikesJob) {
        this.syncIntentFactory = syncIntentFactory;
        this.lazySyncTrackLikesJob =  lazySyncTrackLikesJob;
        this.lazySyncPlaylistLikesJob = lazySyncPlaylistLikesJob;
    }

    public SyncRequest create(Intent intent) {

        if (SyncActions.SYNC_TRACK_LIKES.equals(intent.getAction())) {
            return new SingleJobRequest(lazySyncTrackLikesJob.get(), true);

        } else if (SyncActions.SYNC_PLAYLIST_LIKES.equals(intent.getAction())) {
            return new SingleJobRequest(lazySyncPlaylistLikesJob.get(), true);
        }

        return syncIntentFactory.create(intent);
    }
}
