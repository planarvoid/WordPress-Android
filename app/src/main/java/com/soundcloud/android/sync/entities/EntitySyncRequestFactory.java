package com.soundcloud.android.sync.entities;

import static com.soundcloud.java.checks.Preconditions.checkArgument;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.rx.eventbus.EventBus;

import android.os.ResultReceiver;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.List;

public class EntitySyncRequestFactory {
    private final Provider<EntitySyncJob> tracksSyncJob;
    private final Provider<EntitySyncJob> playlistsSyncJob;
    private final Provider<EntitySyncJob> usersSyncJob;
    private final EventBus eventBus;

    @Inject
    public EntitySyncRequestFactory(@Named(EntitySyncModule.TRACKS_SYNC) Provider<EntitySyncJob> trackSyncJob,
                                    @Named(EntitySyncModule.PLAYLISTS_SYNC) Provider<EntitySyncJob> playlistsSyncJob,
                                    @Named(EntitySyncModule.USERS_SYNC) Provider<EntitySyncJob> usersSyncJob,
                                    EventBus eventBus) {
        this.tracksSyncJob = trackSyncJob;
        this.playlistsSyncJob = playlistsSyncJob;
        this.usersSyncJob = usersSyncJob;
        this.eventBus = eventBus;
    }

    public EntitySyncRequest create(Syncable syncable, List<Urn> entities, ResultReceiver resultReceiver) {
        checkArgument(entities != null, "Requested a resource sync without providing urns...");
        final EntitySyncJob syncJob = getEntitySyncJob(syncable);
        syncJob.setUrns(entities);
        return new EntitySyncRequest(syncJob, syncable, eventBus, resultReceiver);
    }

    private EntitySyncJob getEntitySyncJob(Syncable syncable) {
        switch (syncable) {
            case TRACKS:
                return tracksSyncJob.get();
            case USERS:
                return usersSyncJob.get();
            case PLAYLISTS:
                return playlistsSyncJob.get();
            default:
                throw new IllegalArgumentException("Unexpected syncable : " + syncable);
        }
    }
}
