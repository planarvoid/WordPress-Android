package com.soundcloud.android.sync;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApiSyncServiceTest {

    @Mock private SyncRequestFactory syncRequestFactory;
    @Mock private SyncStateStorage syncStateStorage;

    private ApiSyncService apiSyncService;

    @Before
    public void setUp() throws Exception {
        apiSyncService = new ApiSyncService(syncRequestFactory, syncStateStorage);
    }

    @Test
    public void storeSyncableSyncedOnSuccess() {
        final TestSyncJob syncJob = new TestSyncJob(Optional.of(Syncable.CHARTS), true);
        apiSyncService.onSyncJobCompleted(syncJob);

        verify(syncStateStorage).synced(Syncable.CHARTS);
    }

    @Test
    public void doNotStoreSyncableSyncedOnFailure() {
        final TestSyncJob syncJob = new TestSyncJob(Optional.of(Syncable.CHARTS), false);
        apiSyncService.onSyncJobCompleted(syncJob);

        verify(syncStateStorage, never()).synced(Syncable.CHARTS);
    }

    @Test
    public void doNotStoreSyncableSyncedWhenAbsent() {
        final TestSyncJob syncJob = new TestSyncJob(Optional.<Syncable>absent(), true);
        apiSyncService.onSyncJobCompleted(syncJob);

        verify(syncStateStorage, never()).synced(Syncable.CHARTS);
    }

    private static class TestSyncJob implements SyncJob {
        private final Optional<Syncable> syncable;
        private final boolean waSuccess;

        private TestSyncJob(Optional<Syncable> syncable, boolean waSuccess) {
            this.syncable = syncable;
            this.waSuccess = waSuccess;
        }

        @Override
        public void run() {

        }

        @Override
        public void onQueued() {

        }

        @Override
        public boolean resultedInAChange() {
            return false;
        }

        @Override
        public Exception getException() {
            return null;
        }

        @Override
        public Optional<Syncable> getSyncable() {
            return syncable;
        }

        @Override
        public boolean wasSuccess() {
            return waSuccess;
        }
    }
}
