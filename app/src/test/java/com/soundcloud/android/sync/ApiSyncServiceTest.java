package com.soundcloud.android.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;

public class ApiSyncServiceTest extends AndroidUnitTest {

    @Mock private SyncRequestFactory syncRequestFactory;
    @Mock private SyncStateStorage syncStateStorage;
    @Mock private UnauthorisedRequestRegistry unauthorisedRequestRegistry;

    private ApiSyncService apiSyncService;

    @Before
    public void setUp() throws Exception {
        apiSyncService = new ApiSyncService(syncRequestFactory, syncStateStorage, unauthorisedRequestRegistry);
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

    @Test
    public void shouldEnqueueJobsWhenNoExtraIsDefinedEvenIfTokenIsInvalid() {
        when(unauthorisedRequestRegistry.timeSinceFirstUnauthorisedRequestIsBeyondLimit())
                .thenReturn(true);

        assertThat(apiSyncService.shouldEnqueueJobs(new Intent())).isTrue();
    }

    @Test
    public void shouldEnqueueJobsWhenIsUiRequestExtraIsNotDefinedEvenIfTokenIsInvalid() {
        Intent intent = new Intent();
        intent.putExtra("SOME_EXTRA", true);
        when(unauthorisedRequestRegistry.timeSinceFirstUnauthorisedRequestIsBeyondLimit())
                .thenReturn(true);

        assertThat(apiSyncService.shouldEnqueueJobs(intent)).isTrue();
    }

    @Test
    public void shouldEnqueueJobsForUIRequestsWhenAuthTokenIsValid() {
        Intent intent = new Intent();
        intent.putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true);
        when(unauthorisedRequestRegistry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).thenReturn(false);

        assertThat(apiSyncService.shouldEnqueueJobs(intent)).isTrue();
    }

    @Test
    public void shouldEnqueueJobsForUIRequestsWhenAuthTokenIsInvalid() {
        Intent intent = new Intent();
        intent.putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true);
        when(unauthorisedRequestRegistry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).thenReturn(true);

        assertThat(apiSyncService.shouldEnqueueJobs(intent)).isTrue();
    }

    @Test
    public void shouldEnqueueJobsForNonUIRequestsWhenAuthTokenIsValid() {
        Intent intent = new Intent();
        intent.putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false);
        when(unauthorisedRequestRegistry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).thenReturn(false);

        assertThat(apiSyncService.shouldEnqueueJobs(intent)).isTrue();
    }

    @Test
    public void shouldNotEnqueueSyncJobsForNonUIRequestsWhenAuthTokenIsInvalid() {
        Intent intent = new Intent();
        intent.putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false);
        when(unauthorisedRequestRegistry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).thenReturn(true);

        assertThat(apiSyncService.shouldEnqueueJobs(intent)).isFalse();
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
