package com.soundcloud.android.sync;

public interface SyncJob extends Runnable {
    void onQueued();
    boolean resultedInAChange();
    Exception getException();
}
