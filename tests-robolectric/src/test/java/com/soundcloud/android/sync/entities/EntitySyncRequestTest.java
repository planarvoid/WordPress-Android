package com.soundcloud.android.sync.entities;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncExtras;
import com.soundcloud.android.sync.SyncJob;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Intent;
import android.os.ResultReceiver;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class EntitySyncRequestTest {

    private static final Urn URN = Urn.forTrack(123L);
    private static final String ACTION = "action";

    private EntitySyncRequest entitySyncRequest;

    @Mock private EntitySyncJob entitySyncJob;
    @Mock private EntitySyncJob playlistsSyncer;
    @Mock private ResultReceiver resultReceiver;

    private TestEventBus eventBus = new TestEventBus();
    private Intent intent;

    @Before
    public void setUp() throws Exception {
        intent = new Intent(SyncActions.SYNC_TRACKS);
        intent.putParcelableArrayListExtra(SyncExtras.URNS, newArrayList(URN));
        entitySyncRequest = new EntitySyncRequest(entitySyncJob, intent, eventBus, ACTION, resultReceiver);
    }

    @Test
    public void isAlwaysHighPriorityForNow() throws Exception {
        expect(entitySyncRequest.isHighPriority()).toBeTrue();
    }

    @Test
    public void createsPendingSync() throws Exception {
        expect(entitySyncRequest.getPendingJobs()).toNumber(1);
        verify(entitySyncJob).setUrns(Arrays.asList(URN));
    }

    @Test
    public void getPendingJobsIsEmptyAfterProcessing() throws Exception {
        final SyncJob job = entitySyncRequest.getPendingJobs().iterator().next();
        entitySyncRequest.processJobResult(job);
        expect(entitySyncRequest.getPendingJobs()).toBeEmpty();
    }

    @Test
    public void isWaitingForPendingTracksJob() throws Exception {
        final SyncJob job = entitySyncRequest.getPendingJobs().iterator().next();
        expect(entitySyncRequest.isWaitingForJob(job)).toBeTrue();
    }

    @Test
    public void isWaitingForPendingTracksJobFalseAfterProcessingJob() throws Exception {
        final SyncJob job = entitySyncRequest.getPendingJobs().iterator().next();
        entitySyncRequest.processJobResult(job);
        expect(entitySyncRequest.isWaitingForJob(job)).toBeFalse();
    }

    @Test
    public void isNotSatisfiedBeforeProcessingPendingTrackJob() throws Exception {
        expect(entitySyncRequest.isSatisfied()).toBeFalse();
    }

    @Test
    public void isSatisfiedAfterProcessingTrackJob() throws Exception {
        final SyncJob job = entitySyncRequest.getPendingJobs().iterator().next();
        entitySyncRequest.processJobResult(job);

        expect(entitySyncRequest.isSatisfied()).toBeTrue();
    }

    @Test
    public void finishBroadcastsAnUpdatedTracksCollection() throws Exception {
        final PropertySet propertySet1 = TestPropertySets.fromApiTrack();
        final PropertySet propertySet2 = TestPropertySets.fromApiTrack();
        when(entitySyncJob.getUpdatedEntities()).thenReturn(newArrayList(propertySet1, propertySet2));

        entitySyncRequest.finish();

        final EntityStateChangedEvent entityStateChangedEvent = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        final Map<Urn, PropertySet> changeSet = entityStateChangedEvent.getChangeMap();
        expect(changeSet.size()).toEqual(2);
        expect(changeSet.get(propertySet1.get(EntityProperty.URN))).toBe(propertySet1);
        expect(changeSet.get(propertySet2.get(EntityProperty.URN))).toBe(propertySet2);
    }

    @Test // github #2779
    public void finishDoesNotBroadcastWhenNoChangesReceived() throws Exception {
        when(entitySyncJob.getUpdatedEntities()).thenReturn(Collections.<PropertySet>emptyList());

        entitySyncRequest.finish();

        eventBus.verifyNoEventsOn(EventQueue.ENTITY_STATE_CHANGED);
    }
}