package com.soundcloud.android.sync;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class NewSyncOperationsTest {

    private static final Syncable SYNCABLE = Syncable.CHARTS;
    private static final SyncJobResult JOB_SUCCESS = SyncJobResult.success(SYNCABLE.name(), true);
    private static final long SYNCABLE_STALE_TIME = TimeUnit.DAYS.toMillis(1);

    private NewSyncOperations syncOperations;

    @Mock private SyncInitiator syncInitiator;
    @Mock private SyncStateStorage syncStateStorage;
    @Mock private SyncerRegistry syncerRegistry;

    private TestObserver<Object> observer = new TestObserver<>();

    @Before
    public void setUp() throws Exception {
        syncOperations = new NewSyncOperations(syncInitiator, syncStateStorage, syncerRegistry);

        when(syncInitiator.sync(SYNCABLE)).thenReturn(Single.just(JOB_SUCCESS));
        when(syncerRegistry.get(SYNCABLE)).thenReturn(TestSyncData.from(
                SYNCABLE,
                false,
                SYNCABLE_STALE_TIME,
                true
        ));
    }

    @Test
    public void syncEmitsSyncedState() {
        syncOperations.sync(SYNCABLE).subscribe(observer);

        observer.assertValue(SyncResult.synced());
    }

    @Test
    public void syncEmitsErrorStateWithExceptionOnFailure() {
        final Exception exception = new Exception("SYNC FAILED");
        when(syncInitiator.sync(SYNCABLE)).thenReturn(Single.error(exception));

        syncOperations.sync(SYNCABLE).subscribe(observer);

        observer.assertNoErrors().assertValue(SyncResult.error(exception));
    }

    @Test
    public void lazySyncIfStaleEmitsSyncedStateIfNeverSynced() {
        when(syncStateStorage.hasSyncedBefore(SYNCABLE)).thenReturn(false);
        syncOperations.lazySyncIfStale(SYNCABLE).subscribe(observer);

        observer.assertValue(SyncResult.synced());
    }

    @Test
    public void lazySyncIfStaleSyncsAndEmitsSyncingStateWhenStaleContent() {
        when(syncStateStorage.hasSyncedBefore(SYNCABLE)).thenReturn(true);
        when(syncStateStorage.hasSyncedWithin(SYNCABLE, SYNCABLE_STALE_TIME)).thenReturn(false);

        syncOperations.lazySyncIfStale(SYNCABLE).subscribe(observer);

        observer.assertValue(SyncResult.syncing());
        verify(syncInitiator).sync(SYNCABLE);
    }

    @Test
    public void lazySyncIfStaleDoesNotSyncAndEmitsNoOpStateWhenContentNotStale() {
        when(syncStateStorage.hasSyncedBefore(SYNCABLE)).thenReturn(true);
        when(syncStateStorage.hasSyncedWithin(SYNCABLE, SYNCABLE_STALE_TIME)).thenReturn(true);

        syncOperations.lazySyncIfStale(SYNCABLE).subscribe(observer);

        observer.assertValue(SyncResult.noOp());
        verify(syncInitiator, never()).sync(any());
    }
}
