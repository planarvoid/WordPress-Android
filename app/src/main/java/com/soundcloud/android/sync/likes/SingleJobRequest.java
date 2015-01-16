package com.soundcloud.android.sync.likes;

import com.soundcloud.android.sync.SyncJob;
import com.soundcloud.android.sync.SyncRequest;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;

public class SingleJobRequest implements SyncRequest<SyncJob>{

    private final SyncJob syncJob;
    private final boolean isHighPriority;

    private boolean processed;

    @Inject
    public SingleJobRequest(SyncJob syncJob, boolean isHighPriority) {
        this.syncJob = syncJob;
        this.isHighPriority = isHighPriority;
    }

    @Override
    public boolean isHighPriority() {
        return isHighPriority;
    }

    @Override
    public Collection<? extends SyncJob> getPendingJobs() {
        return Arrays.asList(syncJob);
    }

    @Override
    public boolean isWaitingForJob(SyncJob syncJob) {
        // do not reverse these equals. it is not commutative in this case. Currently not easy to test
        return this.syncJob.equals(syncJob) && !processed;
    }

    @Override
    public void processJobResult(SyncJob syncJob) {
        processed = true;
    }

    @Override
    public boolean isSatisfied() {
        return processed;
    }

    @Override
    public void finish() {
        // ToDo : probably fire an event with the results from the SyncItem
    }
}
