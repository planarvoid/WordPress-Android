package com.soundcloud.android.sync.entities;

import com.google.common.base.Preconditions;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.sync.SyncExtras;
import com.soundcloud.android.sync.SyncJob;
import com.soundcloud.android.sync.SyncRequest;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;

import android.content.Intent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class EntitySyncRequest implements SyncRequest {

    private final EntitySyncJob entitySyncJob;
    private final EventBus eventBus;
    private boolean isSatisfied;

    EntitySyncRequest(EntitySyncJob entitySyncJob, Intent intent, EventBus eventBus) {
        this.entitySyncJob = entitySyncJob;
        this.eventBus = eventBus;
        setUrnsFromIntent(intent);
    }

    private void setUrnsFromIntent(Intent intent) {
        final List<Urn> urnsToSync = intent.getParcelableArrayListExtra(SyncExtras.URNS);
        Preconditions.checkArgument(urnsToSync != null,
                "Requested a resource sync without providing urns...");
        entitySyncJob.setUrns(urnsToSync);
    }


    @Override
    public boolean isHighPriority() {
        // currently only fired from the UI for now, so always high priority
        return true;
    }

    @Override
    public Collection<? extends SyncJob> getPendingJobs() {
        return isSatisfied ? Collections.<SyncJob>emptyList() : Arrays.asList(entitySyncJob);
    }

    @Override
    public boolean isWaitingForJob(@NotNull SyncJob syncJob) {
        return syncJob.equals(entitySyncJob) && !isSatisfied;
    }

    @Override
    public void processJobResult(SyncJob syncJob) {
        if (syncJob.equals(entitySyncJob)) {
            isSatisfied = true;
        }
    }

    @Override
    public boolean isSatisfied() {
        return isSatisfied;
    }

    @Override
    public void finish() {
        final Collection<PropertySet> updatedEntities = entitySyncJob.getUpdatedEntities();
        if (!updatedEntities.isEmpty()) {
            eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromSync(entitySyncJob.getUpdatedEntities()));
        }
    }

}
