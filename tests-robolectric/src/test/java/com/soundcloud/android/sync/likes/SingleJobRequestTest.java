package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.sync.*;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.os.Bundle;
import android.os.ResultReceiver;

import java.util.Collection;

@RunWith(SoundCloudTestRunner.class)
public class SingleJobRequestTest extends TestCase {

    private final String ACTION = "action";

    private com.soundcloud.android.sync.SingleJobRequest singleJobRequest;

    @Mock private DefaultSyncJob syncJob;
    @Mock private ResultReceiver resultReceiver;

    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        singleJobRequest = new com.soundcloud.android.sync.SingleJobRequest(syncJob, ACTION, true, resultReceiver, eventBus);
    }

    @Test
    public void isHighPriorityReturnsValueFromConstructor() throws Exception {
        expect(singleJobRequest.isHighPriority()).toBeTrue();
    }

    @Test
    public void getPendingSyncItemShouldReturnSyncJobWhenNotExecuted() throws Exception {
        Collection<? extends SyncJob> jobs = singleJobRequest.getPendingJobs();
        expect(jobs.contains(syncJob)).toBeTrue();
        expect(jobs).toNumber(1);
    }

    @Test
    public void waitingForSyncItemTrueForSyncJob() throws Exception {
        expect(singleJobRequest.isWaitingForJob(syncJob)).toBeTrue();
    }

    @Test
    public void waitingForSyncItemFalseAfterProcessingSyncJob() throws Exception {
        singleJobRequest.processJobResult(syncJob);
        expect(singleJobRequest.isWaitingForJob(syncJob)).toBeFalse();
    }

    @Test
    public void isSatisfiedIsFalseBeforeProcessingSyncJob() throws Exception {
        expect(singleJobRequest.isSatisfied()).toBeFalse();
    }

    @Test
    public void isSatisfiedIsTrueAfterProcessingSyncJob() throws Exception {
        singleJobRequest.processJobResult(syncJob);
        expect(singleJobRequest.isSatisfied()).toBeTrue();
    }

    @Test
    public void finishSendsSuccessChangedResultThroughResultReceiver() throws Exception {
        when(syncJob.wasSuccess()).thenReturn(true);
        when(syncJob.resultedInAChange()).thenReturn(true);
        singleJobRequest.processJobResult(syncJob);
        singleJobRequest.finish();

        final SyncResult syncResult = getSyncResult();
        expect(syncResult.wasSuccess()).toBeTrue();
        expect(syncResult.wasChanged()).toBeTrue();
    }

    @Test
    public void finishSendsSuccessUnchangedResultThroughResultReceiver() throws Exception {
        when(syncJob.wasSuccess()).thenReturn(true);
        singleJobRequest.processJobResult(syncJob);
        singleJobRequest.finish();

        final SyncResult syncResult = getSyncResult();
        expect(syncResult.wasSuccess()).toBeTrue();
        expect(syncResult.wasChanged()).toBeFalse();
    }

    @Test
    public void finishSendsSuccessUnhangedResultOnEventBus() throws Exception {
        singleJobRequest.processJobResult(syncJob);
        singleJobRequest.finish();

        expect(eventBus.lastEventOn(EventQueue.SYNC_RESULT)).toEqual(SyncResult.success(ACTION, false));
    }

    @Test
    public void finishSendsSuccessChangedResultOnEventBus() throws Exception {
        when(syncJob.resultedInAChange()).thenReturn(true);

        singleJobRequest.processJobResult(syncJob);
        singleJobRequest.finish();

        expect(eventBus.lastEventOn(EventQueue.SYNC_RESULT)).toEqual(SyncResult.success(ACTION, true));
    }

    @Test
    public void finishSendsFailureResultOnEventBus() throws Exception {
        final RuntimeException exception = new RuntimeException();
        when(syncJob.getException()).thenReturn(exception);

        singleJobRequest.processJobResult(syncJob);
        singleJobRequest.finish();

        expect(eventBus.lastEventOn(EventQueue.SYNC_RESULT)).toEqual(SyncResult.failure(ACTION, exception));
    }

    private SyncResult getSyncResult() {
        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(resultReceiver).send(eq(0), bundleCaptor.capture());
        return bundleCaptor.getValue().getParcelable(ResultReceiverAdapter.SYNC_RESULT);
    }
}