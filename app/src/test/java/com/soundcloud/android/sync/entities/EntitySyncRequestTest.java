package com.soundcloud.android.sync.entities;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncJob;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.os.ResultReceiver;

import java.util.Collections;
import java.util.Map;

public class EntitySyncRequestTest extends AndroidUnitTest {

    private static final Urn URN = Urn.forTrack(123L);
    private static final String ACTION = "action";
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
        final TrackItem trackItem1 = TestPropertySets.fromApiTrack();
        final TrackItem trackItem2 = TestPropertySets.fromApiTrack();
        when(entitySyncJob.getUpdatedEntities()).thenReturn(newArrayList(trackItem1.toUpdateEvent(), trackItem2.toUpdateEvent()));

        entitySyncRequest.finish();

        final EntityStateChangedEvent entityStateChangedEvent = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        final Map<Urn, PropertySet> changeSet = entityStateChangedEvent.getChangeMap();
        assertThat(changeSet).hasSize(2);
        assertThat(TrackItem.from(changeSet.get(trackItem1.getUrn()))).isEqualTo(trackItem1);
        assertThat(TrackItem.from(changeSet.get(trackItem2.getUrn()))).isEqualTo(trackItem2);
    }

    @Test // github #2779
    public void finishDoesNotBroadcastWhenNoChangesReceived() throws Exception {
        when(entitySyncJob.getUpdatedEntities()).thenReturn(Collections.emptyList());

        entitySyncRequest.finish();

        eventBus.verifyNoEventsOn(EventQueue.ENTITY_STATE_CHANGED);
    }
}
