package com.soundcloud.android.sync;

import android.os.Bundle;

public class SyncFailedException extends Exception {
    public SyncFailedException(Bundle resultData) {
        super("Sync failed with result " + resultData.getParcelable(ApiSyncService.EXTRA_SYNC_RESULT));
    }

    public SyncFailedException() {
        super("Sync failed");
    }
}
