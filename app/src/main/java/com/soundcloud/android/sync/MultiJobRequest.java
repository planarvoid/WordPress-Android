package com.soundcloud.android.sync;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.rx.eventbus.EventBus;

import android.os.Bundle;
import android.os.ResultReceiver;

import java.util.HashSet;
import java.util.List;

@AutoFactory(allowSubclasses = true)
public class MultiJobRequest implements SyncRequest {

    private final List<SyncJob> syncJobs;
    private final boolean isHighPriority;
        private final ResultReceiver resultReceiver;
    private final EventBus eventBus;

    private final HashSet<SyncJob> jobsRemaining;
    private Bundle resultBundle = new Bundle();

    public MultiJobRequest(List<SyncJob> syncJobs,
                           ResultReceiver resultReceiver,
                           boolean isHighPriority,
                           @Provided EventBus eventBus) {
        this.syncJobs = syncJobs;
        this.isHighPriority = isHighPriority;
        this.resultReceiver = resultReceiver;
        this.eventBus = eventBus;

        jobsRemaining = new HashSet<>(syncJobs);
    }

    @Override
    public boolean isHighPriority() {
        return isHighPriority;
    }

    @Override
    public List<? extends SyncJob> getPendingJobs() {
        return syncJobs;
    }

    @Override
    public boolean isWaitingForJob(SyncJob syncJob) {
        return jobsRemaining.contains(syncJob);
    }

    @Override
    public void processJobResult(SyncJob syncJob) {
        if (isWaitingForJob(syncJob)) {

            jobsRemaining.remove(syncJob);

            final Exception exception = syncJob.getException();
            final String syncableName = getSyncableName(syncJob);
            final SyncJobResult resultEvent = exception == null ?
                                              SyncJobResult.success(syncableName, syncJob.resultedInAChange())
                                                                :
                                              SyncJobResult.failure(syncableName, syncJob.getException());

            resultBundle.putParcelable(syncableName, resultEvent);

            // todo: this will fire N times for N requests waiting on the same job. Move this to ApiSyncService later
            eventBus.publish(EventQueue.SYNC_RESULT, resultEvent);
        }
    }

    public String getSyncableName(SyncJob syncJob) {
        // We want this to throw. Absent syncables will only use the other (legacy) path
        return syncJob.getSyncable().get().name();
    }

    @Override
    public boolean isSatisfied() {
        return jobsRemaining.isEmpty();
    }

    @Override
    public void finish() {
        resultReceiver.send(0, resultBundle);
    }
}
