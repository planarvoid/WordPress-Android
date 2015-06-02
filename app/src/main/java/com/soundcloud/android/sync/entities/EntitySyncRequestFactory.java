package com.soundcloud.android.sync.entities;

import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.sync.SyncActions;
import dagger.Lazy;

import android.content.Intent;
import android.os.ResultReceiver;

import javax.inject.Inject;
import javax.inject.Named;

public class EntitySyncRequestFactory {
    private final Lazy<EntitySyncJob> tracksSyncJob;
    private final Lazy<EntitySyncJob> playlistSyncJob;
    private final Lazy<EntitySyncJob> usersSyncJob;
    private final EventBus eventBus;

    @Inject
    public EntitySyncRequestFactory(@Named(EntitySyncModule.TRACKS_SYNC) Lazy<EntitySyncJob> trackSyncJob,
                                    @Named(EntitySyncModule.PLAYLISTS_SYNC) Lazy<EntitySyncJob> playlistSyncJob,
                                    @Named(EntitySyncModule.USERS_SYNC) Lazy<EntitySyncJob> usersSyncJob, EventBus eventBus) {
        this.tracksSyncJob = trackSyncJob;
        this.playlistSyncJob = playlistSyncJob;
        this.usersSyncJob = usersSyncJob;
        this.eventBus = eventBus;
    }

    public EntitySyncRequest create(Intent intent, ResultReceiver resultReceiver) {
        switch (intent.getAction()) {
            case SyncActions.SYNC_TRACKS:
                return new EntitySyncRequest(tracksSyncJob.get(), intent, eventBus, intent.getAction(), resultReceiver);
            case SyncActions.SYNC_USERS:
                return new EntitySyncRequest(usersSyncJob.get(), intent, eventBus, intent.getAction(), resultReceiver);
            case SyncActions.SYNC_PLAYLISTS:
                return new EntitySyncRequest(playlistSyncJob.get(), intent, eventBus, intent.getAction(), resultReceiver);
            default:
                throw new IllegalArgumentException("Unexpected action : " + intent.getAction());
        }
    }
}
