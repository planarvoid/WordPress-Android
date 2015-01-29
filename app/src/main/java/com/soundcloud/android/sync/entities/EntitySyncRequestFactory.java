package com.soundcloud.android.sync.entities;

import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.sync.SyncActions;
import dagger.Lazy;

import android.content.Intent;

import javax.inject.Inject;
import javax.inject.Named;

public class EntitySyncRequestFactory {
    private final Lazy<EntitySyncJob> tracksSyncJob;
    private final Lazy<EntitySyncJob> playlistSyncJob;
    private final EventBus eventBus;

    @Inject
    public EntitySyncRequestFactory(@Named("TracksSyncJob") Lazy<EntitySyncJob> trackSyncJob,
                                    @Named("PlaylistsSyncJob") Lazy<EntitySyncJob> playlistSyncJob, EventBus eventBus) {
        this.tracksSyncJob = trackSyncJob;
        this.playlistSyncJob = playlistSyncJob;
        this.eventBus = eventBus;
    }

    public EntitySyncRequest create(Intent intent){
        return SyncActions.SYNC_TRACKS.equals(intent.getAction())
                ? new EntitySyncRequest(tracksSyncJob.get(), intent, eventBus)
                : new EntitySyncRequest(playlistSyncJob.get(), intent, eventBus);
    }
}
