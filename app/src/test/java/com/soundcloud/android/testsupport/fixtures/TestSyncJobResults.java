package com.soundcloud.android.testsupport.fixtures;


import com.soundcloud.android.sync.SyncJobResult;

public final class TestSyncJobResults {

    public static SyncJobResult successWithChange() {
        return SyncJobResult.success("action", true);
    }

    public static SyncJobResult successWithoutChange() {
        return SyncJobResult.success("action", false);
    }
}
