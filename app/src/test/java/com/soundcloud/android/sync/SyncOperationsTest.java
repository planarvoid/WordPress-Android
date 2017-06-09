package com.soundcloud.android.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.reactivex.Single;
import io.reactivex.subjects.SingleSubject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import rx.observers.TestSubscriber;

import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class SyncOperationsTest {

    private static final Syncable SYNCABLE = Syncable.CHARTS;
    private static final SyncJobResult JOB_SUCCESS = SyncJobResult.success(SYNCABLE.name(), true);
    private static final long SYNCABLE_STALE_TIME = TimeUnit.DAYS.toMillis(1);

    private SyncOperations syncOperations;

    @Mock private SyncInitiator syncInitiator;
    @Mock private SyncStateStorage syncStateStorage;
    @Mock private SyncerRegistry syncerRegistry;

    private TestSubscriber<Object> subscriber = new TestSubscriber<>();
    private SingleSubject<SyncJobResult> jobResult = SingleSubject.create();

    @Before
    public void setUp() throws Exception {
        syncOperations = new SyncOperations(syncInitiator, syncStateStorage, syncerRegistry);

        when(syncInitiator.sync(SYNCABLE)).thenReturn(jobResult);
        when(syncerRegistry.get(SYNCABLE)).thenReturn(TestSyncData.from(
                SYNCABLE,
                false,
                SYNCABLE_STALE_TIME,
                true
        ));
    }

    @Test
    public void syncSyncsThenEmitsIfNeverSynced() {
        syncOperations.sync(SYNCABLE).subscribe(subscriber);

        subscriber.assertNoValues();
        subscriber.assertNoTerminalEvent();

        completeJob();

        subscriber.assertCompleted();
    }

    public void completeJob() {
        jobResult.onSuccess(JOB_SUCCESS);
    }

    @Test
    public void syncIfNeededSyncsThenEmitsIfNeverSynced() {
        syncOperations.sync(SYNCABLE).subscribe(subscriber);

        subscriber.assertNoValues();
        subscriber.assertNoTerminalEvent();

        completeJob();

        subscriber.assertCompleted();
    }

    @Test
    public void syncIfNeededSyncsThenEmitsWhenStaleContent() {
        when(syncStateStorage.hasSyncedWithin(SYNCABLE, SYNCABLE_STALE_TIME)).thenReturn(false);

        syncOperations.syncIfStale(SYNCABLE).subscribe(subscriber);

        subscriber.assertNoValues();
        subscriber.assertNoTerminalEvent();

        completeJob();

        subscriber.assertCompleted();
    }

    @Test
    public void syncIfNeededDoesNotSyncWhenContentNotStale() {
        when(syncStateStorage.hasSyncedWithin(SYNCABLE, SYNCABLE_STALE_TIME)).thenReturn(true);

        syncOperations.syncIfStale(SYNCABLE).subscribe(subscriber);

        subscriber.assertCompleted();
    }

    @Test
    public void lazySyncIfNeededSyncsThenEmitsIfNeverSynced() {
        when(syncStateStorage.hasSyncedBefore(SYNCABLE)).thenReturn(false);
        syncOperations.lazySyncIfStale(SYNCABLE).subscribe(subscriber);

        subscriber.assertNoValues();
        subscriber.assertNoTerminalEvent();

        completeJob();

        subscriber.assertCompleted();
    }

    @Test
    public void lazySyncIfNeededEmitsThenSyncsWhenStaleContent() {
        when(syncStateStorage.hasSyncedBefore(SYNCABLE)).thenReturn(true);
        when(syncStateStorage.hasSyncedWithin(SYNCABLE, SYNCABLE_STALE_TIME)).thenReturn(false);

        syncOperations.lazySyncIfStale(SYNCABLE).subscribe(subscriber);

        subscriber.assertCompleted();

        verify(syncInitiator).syncAndForget(SYNCABLE);
    }

    @Test
    public void lazySyncIfNeededDoesNotSyncWhenContentNotStale() {
        when(syncStateStorage.hasSyncedBefore(SYNCABLE)).thenReturn(true);
        when(syncStateStorage.hasSyncedWithin(SYNCABLE, SYNCABLE_STALE_TIME)).thenReturn(true);

        syncOperations.lazySyncIfStale(SYNCABLE).subscribe(subscriber);

        subscriber.assertCompleted();
    }

    @Test
    public void syncFailsOnException() {
        final Exception exception = new Exception("SYNC FAILED");
        when(syncInitiator.sync(SYNCABLE)).thenReturn(Single.error(exception));

        syncOperations.sync(SYNCABLE).subscribe(subscriber);

        subscriber.assertError(exception);
    }

    @Test
    public void failSafeSyncDoesReturnResultOnException() {
        when(syncInitiator.sync(SYNCABLE)).thenReturn(Single.error(new Exception("SYNC FAILED")));

        syncOperations.failSafeSync(SYNCABLE).subscribe(subscriber);

        subscriber.assertCompleted();
        assertThat(subscriber.getOnNextEvents().get(0)).isEqualTo(SyncOperations.Result.ERROR);
    }
}
