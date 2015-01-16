package com.soundcloud.android.sync;

import java.util.Collection;

public interface SyncRequest<T extends SyncJob> {

    boolean isHighPriority();

    Collection<? extends SyncJob> getPendingJobs();

    boolean isWaitingForJob(T syncJob);

    void processJobResult(T syncJob);

    boolean isSatisfied();

    void finish();
}
