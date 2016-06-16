package com.soundcloud.android.sync;

import com.soundcloud.java.optional.Optional;

public interface SyncJob extends Runnable {
    void onQueued();

    boolean resultedInAChange();

    Exception getException();

    Optional<Syncable> getSyncable();

    boolean wasSuccess();
}
