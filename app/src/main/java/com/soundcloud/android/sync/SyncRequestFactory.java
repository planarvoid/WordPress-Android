package com.soundcloud.android.sync;

import com.soundcloud.android.sync.likes.SyncPlaylistLikesJob;
import com.soundcloud.android.sync.likes.SyncTrackLikesJob;
import com.soundcloud.android.sync.likes.SingleJobRequest;
import dagger.Lazy;

import android.content.Intent;
import android.os.ResultReceiver;

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
            return new SingleJobRequest(lazySyncTrackLikesJob.get(), intent.getAction(),
                    true, getReceiverFromIntent(intent));

        } else if (SyncActions.SYNC_PLAYLIST_LIKES.equals(intent.getAction())) {
            return new SingleJobRequest(lazySyncPlaylistLikesJob.get(), intent.getAction(), true,
                    getReceiverFromIntent(intent));
        }

        return syncIntentFactory.create(intent);
    }

    private ResultReceiver getReceiverFromIntent(Intent intent) {
        return (ResultReceiver) intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER);
    }
}
