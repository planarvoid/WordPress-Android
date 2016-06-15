package com.soundcloud.android.sync;

import static java.util.Collections.singletonList;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.sync.likes.DefaultSyncJob;
import com.soundcloud.rx.eventbus.EventBus;

import android.os.Bundle;
import android.os.ResultReceiver;

import java.util.List;

@AutoFactory( allowSubclasses = true)
public class SingleJobRequest implements SyncRequest {

    private final DefaultSyncJob syncJob;
    private final boolean isHighPriority;
    private final ResultReceiver resultReceiver;
    private final EventBus eventBus;

    protected final String action;
    protected SyncResult resultEvent;

    public SingleJobRequest(SyncerRegistry.SyncData syncData,
                            ResultReceiver resultReceiver,
                            boolean isHighPriority,
                            @Provided EventBus eventBus) {
        this(new DefaultSyncJob(syncData.syncer.get()),
                syncData.id,
                isHighPriority,
                resultReceiver,
                eventBus);
    }

    public SingleJobRequest(DefaultSyncJob syncJob,
                            String action,
                            boolean isHighPriority,
                            ResultReceiver resultReceiver,
                            @Provided EventBus eventBus) {
        this.syncJob = syncJob;
        this.action = action;
        this.isHighPriority = isHighPriority;
        this.resultReceiver = resultReceiver;
        this.eventBus = eventBus;
    }

    @Override
    public boolean isHighPriority() {
        return isHighPriority;
    }

    @Override
    public List<? extends SyncJob> getPendingJobs() {
        return singletonList(syncJob);
    }

    @Override
    public boolean isWaitingForJob(SyncJob syncJob) {
        // do not reverse these equals. it is not commutative in this case. Currently not easy to test
        return this.syncJob.equals(syncJob) && resultEvent == null;
    }

    @Override
    public void processJobResult(SyncJob syncJob) {
        Exception exception = syncJob.getException();
        resultEvent = exception == null ?
                SyncResult.success(action, syncJob.resultedInAChange())
                : SyncResult.failure(action, syncJob.getException());
    }

    @Override
    public boolean isSatisfied() {
        return resultEvent != null;
    }

    @Override
    public void finish() {
        resultReceiver.send(0, getResultBundle());
        eventBus.publish(EventQueue.SYNC_RESULT, resultEvent);
    }

    private Bundle getResultBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(ResultReceiverAdapter.SYNC_RESULT, resultEvent);
        return bundle;
    }
}
