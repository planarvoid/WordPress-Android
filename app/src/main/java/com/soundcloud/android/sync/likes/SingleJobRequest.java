package com.soundcloud.android.sync.likes;

import com.soundcloud.android.sync.ResultReceiverAdapter;
import com.soundcloud.android.sync.SyncJob;
import com.soundcloud.android.sync.SyncRequest;
import com.soundcloud.android.sync.SyncResult;

import android.os.Bundle;
import android.os.ResultReceiver;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;

public class SingleJobRequest implements SyncRequest {

    private final DefaultSyncJob syncJob;
    private final boolean isHighPriority;
    private final String action;
    private final ResultReceiver resultReceiver;

    private SyncResult resultEvent;

    @Inject
    public SingleJobRequest(DefaultSyncJob syncJob, String action, boolean isHighPriority, ResultReceiver resultReceiver) {
        this.syncJob = syncJob;
        this.action = action;
        this.isHighPriority = isHighPriority;
        this.resultReceiver = resultReceiver;
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
    }

    private Bundle getResultBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(ResultReceiverAdapter.SYNC_RESULT, resultEvent);
        return bundle;
    }
}
