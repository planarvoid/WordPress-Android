package com.soundcloud.android.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.sync.likes.DefaultSyncJob;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.os.Bundle;
import android.os.ResultReceiver;

import java.util.Collection;

public class SingleJobRequestTest extends AndroidUnitTest {

    private final String ACTION = "action";

    private SingleJobRequest singleJobRequest;

    @Mock private DefaultSyncJob syncJob;
    @Mock private ResultReceiver resultReceiver;

    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        singleJobRequest = new SingleJobRequest(syncJob, ACTION, true, resultReceiver, eventBus);
    }

    @Test
    public void isHighPriorityReturnsValueFromConstructor() throws Exception {
        assertThat(singleJobRequest.isHighPriority()).isTrue();
    }

    @Test
    public void getPendingSyncItemShouldReturnSyncJobWhenNotExecuted() throws Exception {
        Collection<? extends SyncJob> jobs = singleJobRequest.getPendingJobs();
        assertThat(jobs.contains(syncJob)).isTrue();
        assertThat(jobs).hasSize(1);
    }

    @Test
    public void waitingForSyncItemTrueForSyncJob() throws Exception {
        assertThat(singleJobRequest.isWaitingForJob(syncJob)).isTrue();
    }

    @Test
    public void waitingForSyncItemFalseAfterProcessingSyncJob() throws Exception {
        singleJobRequest.processJobResult(syncJob);
        assertThat(singleJobRequest.isWaitingForJob(syncJob)).isFalse();
    }

    @Test
    public void isSatisfiedIsFalseBeforeProcessingSyncJob() throws Exception {
        assertThat(singleJobRequest.isSatisfied()).isFalse();
    }

    @Test
    public void isSatisfiedIsTrueAfterProcessingSyncJob() throws Exception {
        singleJobRequest.processJobResult(syncJob);
        assertThat(singleJobRequest.isSatisfied()).isTrue();
    }

    @Test
    public void finishSendsSuccessChangedResultThroughResultReceiver() throws Exception {
        when(syncJob.wasSuccess()).thenReturn(true);
        when(syncJob.resultedInAChange()).thenReturn(true);
        singleJobRequest.processJobResult(syncJob);
        singleJobRequest.finish();

        final SyncResult syncResult = getSyncResult();
        assertThat(syncResult.wasSuccess()).isTrue();
        assertThat(syncResult.wasChanged()).isTrue();
    }

    @Test
    public void finishSendsSuccessUnchangedResultThroughResultReceiver() throws Exception {
        when(syncJob.wasSuccess()).thenReturn(true);
        singleJobRequest.processJobResult(syncJob);
        singleJobRequest.finish();

        final SyncResult syncResult = getSyncResult();
        assertThat(syncResult.wasSuccess()).isTrue();
        assertThat(syncResult.wasChanged()).isFalse();
    }

    @Test
    public void finishSendsSuccessUnchangedResultOnEventBus() throws Exception {
        singleJobRequest.processJobResult(syncJob);
        singleJobRequest.finish();

        assertThat(eventBus.lastEventOn(EventQueue.SYNC_RESULT)).isEqualTo(SyncResult.success(ACTION, false));
    }

    @Test
    public void finishSendsSuccessChangedResultOnEventBus() throws Exception {
        when(syncJob.resultedInAChange()).thenReturn(true);

        singleJobRequest.processJobResult(syncJob);
        singleJobRequest.finish();

        assertThat(eventBus.lastEventOn(EventQueue.SYNC_RESULT)).isEqualTo(SyncResult.success(ACTION, true));
    }

    @Test
    public void finishSendsFailureResultOnEventBus() throws Exception {
        final RuntimeException exception = new RuntimeException();
        when(syncJob.getException()).thenReturn(exception);

        singleJobRequest.processJobResult(syncJob);
        singleJobRequest.finish();

        assertThat(eventBus.lastEventOn(EventQueue.SYNC_RESULT)).isEqualTo(SyncResult.failure(ACTION, exception));
    }

    private SyncResult getSyncResult() {
        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(resultReceiver).send(eq(0), bundleCaptor.capture());
        return bundleCaptor.getValue().getParcelable(ResultReceiverAdapter.SYNC_RESULT);
    }
}