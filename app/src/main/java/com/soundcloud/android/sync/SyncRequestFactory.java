package com.soundcloud.android.sync;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.entities.EntitySyncRequestFactory;
import com.soundcloud.android.sync.likes.DefaultSyncJob;
import com.soundcloud.android.sync.likes.SyncPlaylistLikesJob;
import com.soundcloud.android.sync.likes.SyncTrackLikesJob;
import com.soundcloud.android.sync.playlists.SinglePlaylistJobRequest;
import com.soundcloud.android.sync.playlists.SinglePlaylistSyncerFactory;
import com.soundcloud.android.sync.recommendations.RecommendationsSyncer;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Lazy;

import android.content.Intent;
import android.os.ResultReceiver;

import javax.inject.Inject;

class SyncRequestFactory {

    private final LegacySyncRequest.Factory syncIntentFactory;
    private final Lazy<SyncTrackLikesJob> lazySyncTrackLikesJob;
    private final Lazy<SyncPlaylistLikesJob> lazySyncPlaylistLikesJob;
    private final EntitySyncRequestFactory entitySyncRequestFactory;
    private final SinglePlaylistSyncerFactory singlePlaylistSyncerFactory;
    private final Lazy<RecommendationsSyncer> lazyRecommendationSyncer;
    private final EventBus eventBus;

    @Inject
    SyncRequestFactory(LegacySyncRequest.Factory syncIntentFactory,
                       Lazy<SyncTrackLikesJob> lazySyncTrackLikesJob,
                       Lazy<SyncPlaylistLikesJob> lazySyncPlaylistLikesJob,
                       EntitySyncRequestFactory entitySyncRequestFactory,
                       SinglePlaylistSyncerFactory singlePlaylistSyncerFactory,
                       Lazy<RecommendationsSyncer> lazyRecommendationSyncer,
                       EventBus eventBus) {
        this.syncIntentFactory = syncIntentFactory;
        this.lazySyncTrackLikesJob =  lazySyncTrackLikesJob;
        this.lazySyncPlaylistLikesJob = lazySyncPlaylistLikesJob;
        this.entitySyncRequestFactory = entitySyncRequestFactory;
        this.singlePlaylistSyncerFactory = singlePlaylistSyncerFactory;
        this.lazyRecommendationSyncer = lazyRecommendationSyncer;
        this.eventBus = eventBus;
    }

    public SyncRequest create(Intent intent) {

        if (SyncActions.SYNC_TRACK_LIKES.equals(intent.getAction())) {
            return new SingleJobRequest(lazySyncTrackLikesJob.get(), intent.getAction(),
                    true, getReceiverFromIntent(intent), eventBus);

        } else if (SyncActions.SYNC_PLAYLIST_LIKES.equals(intent.getAction())) {
            return new SingleJobRequest(lazySyncPlaylistLikesJob.get(), intent.getAction(), true,
                    getReceiverFromIntent(intent), eventBus);

        } else if (SyncActions.SYNC_TRACKS.equals(intent.getAction())
                || SyncActions.SYNC_PLAYLISTS.equals(intent.getAction())
                || SyncActions.SYNC_USERS.equals(intent.getAction())) {
            return entitySyncRequestFactory.create(intent, getReceiverFromIntent(intent));

        } else if (SyncActions.SYNC_PLAYLIST.equals(intent.getAction())) {
            final Urn playlistUrn = intent.getParcelableExtra(SyncExtras.URN);
            return new SinglePlaylistJobRequest(new DefaultSyncJob(singlePlaylistSyncerFactory.create(playlistUrn)),
                    intent.getAction(), true, getReceiverFromIntent(intent), eventBus, playlistUrn);

        } else if (SyncActions.SYNC_RECOMMENDATIONS.equals(intent.getAction())) {
            return new SingleJobRequest(
                    new DefaultSyncJob(lazyRecommendationSyncer.get()),
                    SyncActions.SYNC_RECOMMENDATIONS,
                    true,
                    getReceiverFromIntent(intent),
                    eventBus
            );
        }
        return syncIntentFactory.create(intent);
    }

    private ResultReceiver getReceiverFromIntent(Intent intent) {
        return (ResultReceiver) intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER);
    }
}
