package com.soundcloud.android.sync;

import com.soundcloud.android.testsupport.TestSyncer;

import java.util.concurrent.Callable;

public class TestSyncData {

    public static SyncerRegistry.SyncProvider get(Syncable syncable) {
        return from(syncable, true, 100, false);
    }

    public static SyncerRegistry.SyncProvider forStaleTime(Syncable syncable, long timeInMs) {
        return from(syncable, false, timeInMs, true);
    }

    public static SyncerRegistry.SyncProvider from(Syncable syncable,
                                                   final Boolean needSync,
                                                   final long staleTime,
                                                   final boolean usePeriodicSync) {

        return new SyncerRegistry.SyncProvider(syncable) {

            private TestSyncer testSyncer = new TestSyncer();

            @Override
            public Callable<Boolean> syncer(String action, boolean isUiRequest) {
                return testSyncer;
            }

            @Override
            public Boolean isOutOfSync() {
                return needSync;
            }

            @Override
            public long staleTime() {
                return staleTime;
            }

            @Override
            public boolean usePeriodicSync() {
                return usePeriodicSync;
            }
        };
    }
}
