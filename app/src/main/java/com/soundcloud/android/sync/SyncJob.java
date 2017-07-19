package com.soundcloud.android.sync;

import com.soundcloud.java.optional.Optional;

public abstract class SyncJob implements Runnable {

    public abstract void onQueued();

    public abstract boolean resultedInAChange();

    public abstract Exception getException();

    public abstract Optional<Syncable> getSyncable();

    public abstract boolean wasSuccess();

    // we want to force sync jobs to implement equals and hashcode to ensure queuing logic (see ApiSyncService)

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();
}
