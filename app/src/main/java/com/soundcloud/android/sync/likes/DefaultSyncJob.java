package com.soundcloud.android.sync.likes;

import com.soundcloud.android.sync.SyncJob;

import java.util.concurrent.Callable;

public class DefaultSyncJob implements SyncJob {

    private final Callable<Boolean> syncer;
    private boolean syncResultChanged;
    private Exception syncException;

    public DefaultSyncJob(Callable<Boolean> syncer) {
        this.syncer = syncer;
    }

    @Override
    public void onQueued() {
        // no-op
    }

    @Override
    public void run() {
        try {
            syncResultChanged = syncer.call();
        } catch (Exception e) {
            syncException = e;
        }
    }

    public boolean resultedInAChange() {
        return syncResultChanged;
    }

    public Exception getSyncException() {
        return syncException;
    }


}
