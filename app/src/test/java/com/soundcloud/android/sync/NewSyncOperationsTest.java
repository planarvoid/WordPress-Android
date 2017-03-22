package com.soundcloud.android.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.PublishSubject;
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
    private PublishSubject<SyncJobResult> jobResult = PublishSubject.create();

    @Before
    public void setUp() throws Exception {
        syncOperations = new NewSyncOperations(syncInitiator, syncStateStorage, syncerRegistry);

        when(syncInitiator.synchronise(SYNCABLE)).thenReturn(jobResult);
        when(syncerRegistry.get(SYNCABLE)).thenReturn(TestSyncData.from(
                SYNCABLE,
                false,
                SYNCABLE_STALE_TIME,
                true
        ));
    }

    @Test
    public void syncSyncsThenEmitsIfNeverSynced() {
        syncOperations.sync(SYNCABLE).subscribe(observer);

        observer.assertNoValues();
        observer.assertNotTerminated();

        completeJob();

        observer.assertComplete();
    }

    public void completeJob() {
        jobResult.onNext(JOB_SUCCESS);
        jobResult.onComplete();
    }

    @Test
    public void syncIfNeededSyncsThenEmitsIfNeverSynced() {
        syncOperations.sync(SYNCABLE).subscribe(observer);

        observer.assertNoValues();
        observer.assertNotTerminated();

        completeJob();

        observer.assertComplete();
    }

    @Test
    public void syncIfNeededSyncsThenEmitsWhenStaleContent() {
        when(syncStateStorage.hasSyncedWithin(SYNCABLE, SYNCABLE_STALE_TIME)).thenReturn(false);

        syncOperations.syncIfStale(SYNCABLE).subscribe(observer);

        observer.assertNoValues();
        observer.assertNotTerminated();

        completeJob();

        observer.assertComplete();
    }

    @Test
    public void syncIfNeededDoesNotSyncWhenContentNotStale() {
        when(syncStateStorage.hasSyncedWithin(SYNCABLE, SYNCABLE_STALE_TIME)).thenReturn(true);

        syncOperations.syncIfStale(SYNCABLE).subscribe(observer);

        observer.assertComplete();
    }

    @Test
    public void lazySyncIfNeededSyncsThenEmitsIfNeverSynced() {
        when(syncStateStorage.hasSyncedBefore(SYNCABLE)).thenReturn(false);
        syncOperations.lazySyncIfStale(SYNCABLE).subscribe(observer);

        observer.assertNoValues();
        observer.assertNotTerminated();

        completeJob();

        observer.assertComplete();
    }

    @Test
    public void lazySyncIfNeededEmitsThenSyncsWhenStaleContent() {
        when(syncStateStorage.hasSyncedBefore(SYNCABLE)).thenReturn(true);
        when(syncStateStorage.hasSyncedWithin(SYNCABLE, SYNCABLE_STALE_TIME)).thenReturn(false);

        syncOperations.lazySyncIfStale(SYNCABLE).subscribe(observer);

        observer.assertComplete();

        assertThat(jobResult.hasObservers()).isTrue();
    }

    @Test
    public void lazySyncIfNeededDoesNotSyncWhenContentNotStale() {
        when(syncStateStorage.hasSyncedBefore(SYNCABLE)).thenReturn(true);
        when(syncStateStorage.hasSyncedWithin(SYNCABLE, SYNCABLE_STALE_TIME)).thenReturn(true);

        syncOperations.lazySyncIfStale(SYNCABLE).subscribe(observer);

        observer.assertComplete();
    }

    @Test
    public void syncFailsOnException() {
        final Exception exception = new Exception("SYNC FAILED");
        when(syncInitiator.synchronise(SYNCABLE)).thenReturn(Observable.error(exception));

        syncOperations.sync(SYNCABLE).subscribe(observer);

        observer.assertError(exception);
    }

    @Test
    public void failSafeSyncDoesReturnResultOnException() {
        when(syncInitiator.synchronise(SYNCABLE)).thenReturn(Observable.error(new Exception("SYNC FAILED")));

        syncOperations.failSafeSync(SYNCABLE).subscribe(observer);

        observer.assertComplete();
        assertThat(observer.values().get(0)).isEqualTo(NewSyncOperations.Result.ERROR);
    }

}
