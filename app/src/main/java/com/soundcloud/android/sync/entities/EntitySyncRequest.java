package com.soundcloud.android.sync.entities;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.sync.ResultReceiverAdapter;
import com.soundcloud.android.sync.SyncJob;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.sync.SyncRequest;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;

class EntitySyncRequest implements SyncRequest {

    private final EntitySyncJob entitySyncJob;
    private final EventBus eventBus;
    private final Syncable syncable;
    private SyncJobResult resultEvent;
    @Nullable private final ResultReceiver resultReceiver;

    EntitySyncRequest(EntitySyncJob entitySyncJob,
                      Syncable syncable,
                      EventBus eventBus,
                      @Nullable ResultReceiver resultReceiver) {
        this.entitySyncJob = entitySyncJob;
        this.eventBus = eventBus;
        this.syncable = syncable;
        this.resultReceiver = resultReceiver;
    }

    @Override
    public boolean isHighPriority() {
        // currently only fired from the UI for now, so always high priority
        return true;
    }

    @Override
    public Collection<? extends SyncJob> getPendingJobs() {
        return isSatisfied() ? Collections.<SyncJob>emptyList() : Collections.singletonList(entitySyncJob);
    }

    @Override
    public boolean isWaitingForJob(@NotNull SyncJob syncJob) {
        return syncJob.equals(entitySyncJob) && !isSatisfied();
    }

    @Override
    public void processJobResult(SyncJob syncJob) {
        if (syncJob.equals(entitySyncJob)) {
            Exception exception = syncJob.getException();
            resultEvent = exception == null ?
                          SyncJobResult.success(syncable.name(), syncJob.resultedInAChange())
                                            : SyncJobResult.failure(syncable.name(), syncJob.getException());
        }
    }

    @Override
    public boolean isSatisfied() {
        return resultEvent != null;
    }

    @Override
    public void finish() {
        if (resultReceiver != null) {
            resultReceiver.send(0, getResultBundle());
        }

        final Collection<EntityStateChangedEvent> updatedEntities = entitySyncJob.getUpdatedEntities();
        if (!updatedEntities.isEmpty()) {
            eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.mergeUpdates(updatedEntities));
        }
    }

    private Bundle getResultBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(ResultReceiverAdapter.SYNC_RESULT, resultEvent);
        return bundle;
    }

}
