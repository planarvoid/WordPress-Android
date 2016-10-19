package com.soundcloud.android.sync;

import static com.soundcloud.android.sync.SyncIntentHelper.getSyncable;
import static com.soundcloud.android.sync.SyncIntentHelper.getSyncables;
import static com.soundcloud.java.checks.Preconditions.checkArgument;

import com.soundcloud.android.events.BackgroundSyncEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.entities.EntitySyncRequestFactory;
import com.soundcloud.android.sync.likes.DefaultSyncJob;
import com.soundcloud.android.sync.likes.SyncPlaylistLikesJob;
import com.soundcloud.android.sync.likes.SyncTrackLikesJob;
import com.soundcloud.android.sync.playlists.SinglePlaylistJobRequest;
import com.soundcloud.android.sync.playlists.SinglePlaylistSyncerFactory;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Lazy;

import android.content.Intent;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

class SyncRequestFactory {

    private final SyncerRegistry syncerRegistry;
    private final SingleJobRequestFactory singleJobRequestFactory;
    private final MultiJobRequestFactory multiJobRequestFactory;
    private final LegacySyncRequest.Factory syncIntentFactory;
    private final Lazy<SyncTrackLikesJob> lazySyncTrackLikesJob;
    private final Lazy<SyncPlaylistLikesJob> lazySyncPlaylistLikesJob;
    private final EntitySyncRequestFactory entitySyncRequestFactory;
    private final SinglePlaylistSyncerFactory singlePlaylistSyncerFactory;
    private final EventBus eventBus;

    @Inject
    SyncRequestFactory(
            SyncerRegistry syncerRegistry,
            SingleJobRequestFactory singleJobRequestFactory,
            MultiJobRequestFactory multiJobRequestFactory,
            LegacySyncRequest.Factory syncIntentFactory,
            Lazy<SyncTrackLikesJob> lazySyncTrackLikesJob,
            Lazy<SyncPlaylistLikesJob> lazySyncPlaylistLikesJob,
            EntitySyncRequestFactory entitySyncRequestFactory,
            SinglePlaylistSyncerFactory singlePlaylistSyncerFactory,
            EventBus eventBus) {
        this.syncerRegistry = syncerRegistry;
        this.singleJobRequestFactory = singleJobRequestFactory;
        this.multiJobRequestFactory = multiJobRequestFactory;
        this.syncIntentFactory = syncIntentFactory;
        this.lazySyncTrackLikesJob = lazySyncTrackLikesJob;
        this.lazySyncPlaylistLikesJob = lazySyncPlaylistLikesJob;
        this.entitySyncRequestFactory = entitySyncRequestFactory;
        this.singlePlaylistSyncerFactory = singlePlaylistSyncerFactory;
        this.eventBus = eventBus;
    }

    SyncRequest create(Intent intent) {
        // TODO : Ideally, this should be gone, and we should always just have an array of syncables, and a MultiJobRequest
        // https://soundcloud.atlassian.net/browse/CC-254
        if (intent.hasExtra(ApiSyncService.EXTRA_SYNCABLE)) {
            return createSingleJobRequest(intent);
        } else if (intent.hasExtra(ApiSyncService.EXTRA_SYNCABLES)) {
            return createMultiJobRequest(intent);
        } else {
            return createLegacyRequest(intent);
        }
    }

    private SyncRequest createSingleJobRequest(Intent intent) {
        final Syncable syncable = getSyncable(intent);
        switch (syncable) {
            case PLAYLIST:
                return createPlaylistSyncRequest(intent);
            case TRACKS:
            case PLAYLISTS:
            case USERS:
                return createEntitySyncRequest(intent, syncable);
            default:
                return createDefaultSingleJobRequest(intent, syncable);
        }
    }

    private SyncRequest createEntitySyncRequest(Intent intent, Syncable syncable) {
        final List<Urn> syncEntities = SyncIntentHelper.getSyncEntities(intent);
        return entitySyncRequestFactory.create(syncable,
                                               syncEntities, getReceiverFromIntent(intent));
    }

    @NonNull
    private SyncRequest createPlaylistSyncRequest(Intent intent) {
        final List<Urn> requestEntities = SyncIntentHelper.getSyncEntities(intent);
        checkArgument(requestEntities.size() == 1, "Expected 1 playlist urn to sync, received " + requestEntities.size());

        final Urn playlistUrn = requestEntities.get(0);
        return new SinglePlaylistJobRequest(new DefaultSyncJob(singlePlaylistSyncerFactory.create(playlistUrn)),
                                            Syncable.PLAYLIST.name(),
                                            true,
                                            getReceiverFromIntent(intent),
                                            eventBus,
                                            playlistUrn);
    }

    private SyncRequest createDefaultSingleJobRequest(Intent intent, Syncable syncable) {
        final boolean isUiRequest = getIsUiRequest(intent);
        final Callable<Boolean> syncer = syncerRegistry.get(syncable).syncer(intent.getAction(), isUiRequest);
        final DefaultSyncJob syncJob = new DefaultSyncJob(syncer,syncable);
        return singleJobRequestFactory.create(syncJob,
                                              syncable.name(),
                                              isUiRequest,
                                              getReceiverFromIntent(intent));
    }

    private SyncRequest createMultiJobRequest(Intent intent) {
        final List<Syncable> syncables = getSyncables(intent);
        final boolean isUiRequest = getIsUiRequest(intent);
        final List<SyncJob> syncJobs = createSyncJobs(syncables, isUiRequest);
        final ResultReceiver resultReceiver = getReceiverFromIntent(intent);
        logBackgroundSync(syncables, isUiRequest);
        return multiJobRequestFactory.create(syncJobs, resultReceiver, isUiRequest);
    }

    private List<SyncJob> createSyncJobs(List<Syncable> syncables, boolean isUiRequest) {
        final List<SyncJob> syncJobs = new ArrayList<>(syncables.size());
        for (Syncable syncable : syncables) {
            final DefaultSyncJob syncJob = new DefaultSyncJob(syncerRegistry.get(syncable).syncer(null, isUiRequest), syncable);
            syncJobs.add(syncJob);
        }
        return syncJobs;
    }

    private SyncRequest createLegacyRequest(Intent intent) {
        if (LegacySyncActions.SYNC_TRACK_LIKES.equals(intent.getAction())) {
            return new SingleJobRequest(lazySyncTrackLikesJob.get(), intent.getAction(),
                                        true, getReceiverFromIntent(intent), eventBus);

        } else if (LegacySyncActions.SYNC_PLAYLIST_LIKES.equals(intent.getAction())) {
            return new SingleJobRequest(lazySyncPlaylistLikesJob.get(), intent.getAction(), true,
                                        getReceiverFromIntent(intent), eventBus);

        }

        logLegacyBackgroundSync(intent);
        return syncIntentFactory.create(intent);
    }

    private ResultReceiver getReceiverFromIntent(Intent intent) {
        return (ResultReceiver) intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER);
    }

    private boolean getIsUiRequest(Intent intent) {
        // TODO, we should probably default to false when we get rid of LegacySyncInitiator, as it should always be set
        return intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true);
    }

    private void logBackgroundSync(List<Syncable> syncables, boolean isHighPriority) {
        if (!isHighPriority) {
            eventBus.publish(EventQueue.TRACKING, new BackgroundSyncEvent(syncables.size()));
        }
    }

    private void logLegacyBackgroundSync(Intent intent) {
        if (intent.hasExtra(ApiSyncService.EXTRA_SYNC_URIS) && !getIsUiRequest(intent)) {
            int syncUriCount = intent.getParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS).size();
            eventBus.publish(EventQueue.TRACKING, new BackgroundSyncEvent(syncUriCount));
        }
    }
}
