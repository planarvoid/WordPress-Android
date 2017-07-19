package com.soundcloud.android.sync.likes;

import com.soundcloud.android.sync.SyncJob;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.optional.Optional;

import java.util.concurrent.Callable;

public class DefaultSyncJob extends SyncJob {

    private final Callable<Boolean> syncer;
    private final Optional<Syncable> syncableOptional;
    private boolean syncResultChanged;
    private Exception syncException;

    public DefaultSyncJob(Callable<Boolean> syncer) {
        this.syncer = syncer;
        this.syncableOptional = Optional.absent();
    }

    public DefaultSyncJob(Callable<Boolean> syncer, Syncable syncable) {
        this.syncer = syncer;
        this.syncableOptional = Optional.of(syncable);
    }

    @Override
    public void onQueued(){
        // no-op
    }

    @Override
    public void run() {
        try {
            syncResultChanged = syncer.call();
        } catch (Exception e) {
            syncException = e;
            reportNonNetworkError(e);
        }
    }

    private void reportNonNetworkError(Exception e) {
        if (!ErrorUtils.isNetworkError(e)) {
            ErrorUtils.handleSilentException(e);
        }
    }

    public boolean resultedInAChange() {
        return syncResultChanged;
    }

    @Override
    public Exception getException() {
        return syncException;
    }

    @Override
    public Optional<Syncable> getSyncable() {
        return syncableOptional;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DefaultSyncJob)) {
            return false;
        }

        DefaultSyncJob that = (DefaultSyncJob) o;
        return syncableOptional.equals(that.syncableOptional);
    }

    @Override
    public int hashCode() {
        return syncableOptional.hashCode();
    }

    @Override
    public boolean wasSuccess() {
        return syncException == null;
    }
}
