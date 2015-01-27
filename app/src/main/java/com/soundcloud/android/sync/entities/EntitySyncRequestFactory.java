package com.soundcloud.android.sync.entities;

import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.sync.SyncActions;
import dagger.Lazy;

import android.content.Intent;

import javax.inject.Inject;
import javax.inject.Named;

public class EntitySyncRequestFactory {
    private final Lazy<EntitySyncJob> tracksSyncer;
    private final Lazy<EntitySyncJob> playlistSyncer;
    private final EventBus eventBus;

    @Inject
    public EntitySyncRequestFactory(@Named("TracksSyncer") Lazy<EntitySyncJob> tracksSyncer, @Named("PlaylistsSyncer") Lazy<EntitySyncJob> playlistSyncer, EventBus eventBus) {
        this.tracksSyncer = tracksSyncer;
        this.playlistSyncer = playlistSyncer;
        this.eventBus = eventBus;
    }

    public EntitySyncRequest create(Intent intent){
        return SyncActions.SYNC_TRACKS.equals(intent.getAction())
                ? new EntitySyncRequest(tracksSyncer.get(), intent, eventBus)
                : new EntitySyncRequest(playlistSyncer.get(), intent, eventBus);
    }
}
