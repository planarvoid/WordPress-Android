package com.soundcloud.android.sync.entities;

import static com.soundcloud.java.checks.Preconditions.checkArgument;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.sync.ResultReceiverAdapter;
import com.soundcloud.android.sync.SyncExtras;
import com.soundcloud.android.sync.SyncJob;
import com.soundcloud.android.sync.SyncRequest;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.java.collections.PropertySet;
import org.jetbrains.annotations.NotNull;

import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

class EntitySyncRequest implements SyncRequest {

    private final EntitySyncJob entitySyncJob;
    private final EventBus eventBus;
    private final String action;
    private SyncResult resultEvent;
    @Nullable private final ResultReceiver resultReceiver;

    EntitySyncRequest(EntitySyncJob entitySyncJob, Intent intent, EventBus eventBus, String action, @Nullable ResultReceiver resultReceiver) {
        this.entitySyncJob = entitySyncJob;
        this.eventBus = eventBus;
        this.action = action;
        this.resultReceiver = resultReceiver;
        setUrnsFromIntent(intent);
    }

    private void setUrnsFromIntent(Intent intent) {
        final List<Urn> urnsToSync = intent.getParcelableArrayListExtra(SyncExtras.URNS);
        checkArgument(urnsToSync != null,
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
                    SyncResult.success(action, syncJob.resultedInAChange())
                    : SyncResult.failure(action, syncJob.getException());
        }
    }

    @Override
    public boolean isSatisfied() {
        return resultEvent != null;
    }

    @Override
    public void finish() {
        if (resultReceiver != null){
            resultReceiver.send(0, getResultBundle());
        }

        final Collection<PropertySet> updatedEntities = entitySyncJob.getUpdatedEntities();
        if (!updatedEntities.isEmpty()) {
            eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromSync(entitySyncJob.getUpdatedEntities()));
        }
    }

    private Bundle getResultBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(ResultReceiverAdapter.SYNC_RESULT, resultEvent);
        return bundle;
    }

}
