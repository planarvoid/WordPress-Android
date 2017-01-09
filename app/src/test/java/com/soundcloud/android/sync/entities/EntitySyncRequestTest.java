package com.soundcloud.android.sync.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.sync.SyncJob;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.os.ResultReceiver;

public class EntitySyncRequestTest extends AndroidUnitTest {

    private static final Syncable SYNCABLE = Syncable.TRACKS;

    private EntitySyncRequest entitySyncRequest;

    @Mock private EntitySyncJob entitySyncJob;
    @Mock private EntitySyncJob playlistsSyncer;
    @Mock private ResultReceiver resultReceiver;

    private TestEventBus eventBus = new TestEventBus();


    @Before
    public void setUp() throws Exception {
        entitySyncRequest = new EntitySyncRequest(entitySyncJob, SYNCABLE, eventBus, resultReceiver);
    }

    @Test
    public void isAlwaysHighPriorityForNow() throws Exception {
        assertThat(entitySyncRequest.isHighPriority()).isTrue();
    }

    @Test
    public void createsPendingSync() throws Exception {
        assertThat(entitySyncRequest.getPendingJobs()).hasSize(1);
    }

    @Test
    public void getPendingJobsIsEmptyAfterProcessing() throws Exception {
        final SyncJob job = entitySyncRequest.getPendingJobs().iterator().next();
        entitySyncRequest.processJobResult(job);
        assertThat(entitySyncRequest.getPendingJobs()).isEmpty();
    }

    @Test
    public void isWaitingForPendingTracksJob() throws Exception {
        final SyncJob job = entitySyncRequest.getPendingJobs().iterator().next();
        assertThat(entitySyncRequest.isWaitingForJob(job)).isTrue();
    }

    @Test
    public void isWaitingForPendingTracksJobFalseAfterProcessingJob() throws Exception {
        final SyncJob job = entitySyncRequest.getPendingJobs().iterator().next();
        entitySyncRequest.processJobResult(job);
        assertThat(entitySyncRequest.isWaitingForJob(job)).isFalse();
    }

    @Test
    public void isNotSatisfiedBeforeProcessingPendingTrackJob() throws Exception {
        assertThat(entitySyncRequest.isSatisfied()).isFalse();
    }

    @Test
    public void isSatisfiedAfterProcessingTrackJob() throws Exception {
        final SyncJob job = entitySyncRequest.getPendingJobs().iterator().next();
        entitySyncRequest.processJobResult(job);

        assertThat(entitySyncRequest.isSatisfied()).isTrue();
    }

    @Test
    public void finishBroadcastsAnUpdatedTracksCollection() throws Exception {
        entitySyncRequest.finish();

        verify(entitySyncJob).publishSyncEvent();
    }
}
