package com.soundcloud.android.sync;

import java.util.Collection;

public interface SyncRequest {

    boolean isHighPriority();

    Collection<? extends SyncJob> getPendingJobs();

    boolean isWaitingForJob(SyncJob syncJob);

    void processJobResult(SyncJob syncJob);

    boolean isSatisfied();

    void finish();
}
