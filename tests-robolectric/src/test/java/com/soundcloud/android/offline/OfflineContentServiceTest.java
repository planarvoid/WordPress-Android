package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineContentEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import android.content.Intent;
import android.os.Message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class OfflineContentServiceTest {

    @Mock private DownloadOperations downloadOperations;
    @Mock private DownloadNotificationController notificationController;
    @Mock private OfflineContentScheduler offlineContentScheduler;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private DownloadHandler.Builder handlerFactory;
    @Mock private DownloadHandler downloadHandler;

    private final DownloadRequest downloadRequest1 = createDownloadRequest(123L);
    private final DownloadRequest downloadRequest2 = createDownloadRequest(456L);
    private final DownloadRequest downloadRequest3 = createDownloadRequest(789L);
    private final DownloadResult downloadResult1 = DownloadResult.success(Urn.forTrack(123L));
    private final DownloadResult unavailableTrackResult1 = DownloadResult.unavailable(Urn.forTrack(123L));
    private final DownloadResult failedResult1 = DownloadResult.failed(Urn.forTrack(123L));

    private TestObservables.MockObservable<List<Urn>> deletePendingRemoval;
    private TestObservables.MockObservable<Void> updateOfflineContent;
    private OfflineContentService service;
    private TestEventBus eventBus;
    private Message downloadMessage;

    @Before
    public void setUp() {
        deletePendingRemoval = TestObservables.emptyObservable();
        updateOfflineContent = TestObservables.emptyObservable();
        eventBus = new TestEventBus();

        service = new OfflineContentService(downloadOperations, offlineContentOperations, notificationController,
                eventBus, offlineContentScheduler, handlerFactory, Schedulers.immediate());

        when(downloadOperations.deletePendingRemovals()).thenReturn(deletePendingRemoval);
        when(handlerFactory.create(service)).thenReturn(downloadHandler);
        downloadMessage = new Message();
        when(downloadHandler.obtainMessage(eq(DownloadHandler.ACTION_DOWNLOAD), any(Object.class))).thenReturn(downloadMessage);
        when(downloadOperations.isValidNetwork()).thenReturn(true);

        service.onCreate();
    }

    @Test
    public void deletePendingRemovalsWhenStarting() {
        when(offlineContentOperations.loadDownloadRequests()).thenReturn(buildDownloadRequestObservable(downloadRequest1));

        startService();

        expect(deletePendingRemoval.subscribedTo()).toBeTrue();
    }

    @Test
    public void startsSyncingWhenALikedTrackIsNotSynced() {
        when(offlineContentOperations.loadDownloadRequests()).thenReturn(buildDownloadRequestObservable(downloadRequest1));

        startService();

        verify(downloadHandler).sendMessage(downloadMessage);
    }

    @Test
    public void doesNotStartSyncingWhenAllLikedTracksAreSynced() {
        when(offlineContentOperations.loadDownloadRequests()).thenReturn(Observable.<List<DownloadRequest>>empty());

        startService();

        verify(downloadHandler, never()).handleMessage(any(Message.class));
    }

    @Test
    public void sendsDownloadPendingWhenCreatingRequestsQueue() {
        when(offlineContentOperations.loadDownloadRequests()).thenReturn(buildDownloadRequestObservable(downloadRequest1, downloadRequest2));

        startService();

        expectDownloadsPending(eventBus.firstEventOn(EventQueue.ENTITY_STATE_CHANGED), downloadRequest1.urn, downloadRequest2.urn);
    }

    @Test
    public void sendsDownloadPendingWhenUpdatingRequestsQueue() {
        when(offlineContentOperations.loadDownloadRequests()).thenReturn(buildDownloadRequestObservable(downloadRequest1, downloadRequest2));
        startService();

        when(downloadHandler.isDownloading()).thenReturn(true);
        when(downloadHandler.getCurrent()).thenReturn(downloadRequest1);
        when(offlineContentOperations.loadDownloadRequests()).thenReturn(buildDownloadRequestObservable(downloadRequest1, downloadRequest2, downloadRequest3));
        startService();
        service.onSuccess(downloadResult1);

        final List<EntityStateChangedEvent> events = eventBus.eventsOn(EventQueue.ENTITY_STATE_CHANGED);
        expectDownloadsPending(events.get(0),  downloadRequest1.urn, downloadRequest2.urn);
        expectDownloadStarted(events.get(1), downloadRequest1.urn);
        expectDownloadsPending(events.get(2), downloadRequest3.urn);
        expectDownloadFinished(events.get(3), downloadRequest1.urn);
        expectDownloadStarted(events.get(4), downloadRequest2.urn);
    }

    @Test
    public void doesNotSendDownloadRemovedWhenCreatingRequestsQueue() throws Exception {
        when(offlineContentOperations.loadDownloadRequests()).thenReturn(buildDownloadRequestObservable(downloadRequest1));

        startService();

        final List<EntityStateChangedEvent> events = eventBus.eventsOn(EventQueue.ENTITY_STATE_CHANGED);
        expectDownloadsPending(events.get(0), downloadRequest1.urn);
        expectDownloadStarted(events.get(1), downloadRequest1.urn);
    }

    @Test
    public void sendsDownloadRemovedWhenUpdatingRequestsQueue() throws Exception {
        when(offlineContentOperations.loadDownloadRequests()).thenReturn(buildDownloadRequestObservable(downloadRequest1, downloadRequest2));
        startService();

        when(downloadHandler.isDownloading()).thenReturn(true);
        when(downloadHandler.getCurrent()).thenReturn(downloadRequest1);
        when(offlineContentOperations.loadDownloadRequests()).thenReturn(buildDownloadRequestObservable(downloadRequest1));
        startService();

        final List<EntityStateChangedEvent> events = eventBus.eventsOn(EventQueue.ENTITY_STATE_CHANGED);
        expectDownloadsPending(events.get(0), downloadRequest1.urn, downloadRequest2.urn);
        expectDownloadStarted(events.get(1), downloadRequest1.urn);
        expectDownloadsRemoved(events.get(2), downloadRequest2.urn);
    }

    @Test
    public void sendsDownloadStartedEventWhenDownloadingLikedTrack() {
        when(offlineContentOperations.loadDownloadRequests()).thenReturn(buildDownloadRequestObservable(downloadRequest1));

        startService();

        List<OfflineContentEvent> offlineContentEvents = eventBus.eventsOn(EventQueue.OFFLINE_CONTENT);
        expect(offlineContentEvents).toContainExactly(OfflineContentEvent.idle(), OfflineContentEvent.start());

        expectDownloadStarted(eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED), downloadRequest1.urn);
    }

    @Test
    public void sendsStartAndStopEventIfServiceRunsButHasNoTracksToDownload() {
        when(offlineContentOperations.loadDownloadRequests()).thenReturn(Observable.just(Collections.<DownloadRequest>emptyList()));

        startService();

        List<OfflineContentEvent> offlineContentEvents = eventBus.eventsOn(EventQueue.OFFLINE_CONTENT);
        expect(offlineContentEvents).toContainExactly(OfflineContentEvent.idle(), OfflineContentEvent.start(), OfflineContentEvent.stop());
    }

    @Test
    public void cancelServiceEmitsOfflineSyncStopEvent() {
        stopService();

        List<OfflineContentEvent> offlineContentEvents = eventBus.eventsOn(EventQueue.OFFLINE_CONTENT);
        expect(offlineContentEvents).toContainExactly(OfflineContentEvent.idle(), OfflineContentEvent.stop());
    }

    @Test
    public void sendsDownloadStartedEventWhenLikedTrackDownloadStarted() {
        when(offlineContentOperations.loadDownloadRequests()).thenReturn(buildDownloadRequestObservable(downloadRequest1));

        startService();

        expectDownloadStarted(eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED), downloadRequest1.urn);
    }

    @Test
    public void sendsDownloadSucceededEventWhenLikedTrackDownloaded() {
        when(offlineContentOperations.loadDownloadRequests()).thenReturn(buildDownloadRequestObservable(downloadRequest1));

        startService();
        service.onSuccess(downloadResult1);

        final List<OfflineContentEvent> offlineContentEvents = eventBus.eventsOn(EventQueue.OFFLINE_CONTENT);
        expect(offlineContentEvents).toContainExactly(OfflineContentEvent.idle(), OfflineContentEvent.start(), OfflineContentEvent.stop());

        List<EntityStateChangedEvent> entityStateChangedEvents = eventBus.eventsOn(EventQueue.ENTITY_STATE_CHANGED);
        expectDownloadsPending(entityStateChangedEvents.get(0), downloadRequest1.urn);
        expectDownloadStarted(entityStateChangedEvents.get(1), downloadRequest1.urn);
        expectDownloadFinished(entityStateChangedEvents.get(2), downloadRequest1.urn);
        expect(entityStateChangedEvents.get(2).getChangeMap().keySet()).toContainExactly(downloadRequest1.urn);

    }

    @Test
    public void sendsDownloadFailedEventWhenLikedTrackDownloadFailed() {
        when(offlineContentOperations.loadDownloadRequests()).thenReturn(buildDownloadRequestObservable(downloadRequest1));

        startService();
        service.onError(downloadResult1);

        final List<OfflineContentEvent> offlineContentEvents = eventBus.eventsOn(EventQueue.OFFLINE_CONTENT);
        expect(offlineContentEvents).toContainExactly(OfflineContentEvent.idle(), OfflineContentEvent.start(), OfflineContentEvent.stop());

        List<EntityStateChangedEvent> entityStateChangedEvents = eventBus.eventsOn(EventQueue.ENTITY_STATE_CHANGED);
        expectDownloadsPending(entityStateChangedEvents.get(0), downloadRequest1.urn);
        expectDownloadStarted(entityStateChangedEvents.get(1), downloadRequest1.urn);
        expectDownloadFailed(entityStateChangedEvents.get(2), downloadRequest1.urn);
    }

    @Test
    public void stopAndScheduleRetryWhenTrackDownloadFailed() {
        when(offlineContentOperations.loadDownloadRequests()).thenReturn(buildDownloadRequestObservable(downloadRequest1));

        startService();
        service.onError(failedResult1);

        verify(offlineContentScheduler).scheduleRetry();
        verify(downloadHandler).quit();
    }

    @Test
    public void connectionErrorNotificationWhenTrackDownloadFailed() {
        when(offlineContentOperations.loadDownloadRequests()).thenReturn(buildDownloadRequestObservable(downloadRequest1));

        startService();
        service.onError(failedResult1);

        verify(notificationController).onConnectionError();
    }

    @Test
    public void continueDownloadNextTrackWhenTrackUnavailableForDownload() {
        when(offlineContentOperations.loadDownloadRequests()).thenReturn(buildDownloadRequestObservable(downloadRequest1, downloadRequest2));

        startService();
        service.onError(unavailableTrackResult1);

        verify(notificationController).onDownloadError();
        verify(downloadHandler, times(2)).sendMessage(downloadMessage);
    }

    @Test
    public void stopAndScheduleRetryWhenNoValidNetworkOnNewDownload() {
        when(offlineContentOperations.loadDownloadRequests()).thenReturn(buildDownloadRequestObservable(downloadRequest1, downloadRequest2));

        startService();
        when(downloadOperations.isValidNetwork()).thenReturn(false);
        service.onSuccess(unavailableTrackResult1);

        verify(offlineContentScheduler).scheduleRetry();
        verify(downloadHandler).quit();
    }

    @Test
    public void showsNotificationWhenDownloadingLikedTrack() {
        when(offlineContentOperations.loadDownloadRequests()).thenReturn(buildDownloadRequestObservable(downloadRequest1, downloadRequest2));

        startService();

        verify(notificationController).onPendingRequests(2);
    }

    @Test
    public void updatesNotificationWhenAnotherTrackLikedDuringDownload() {
        when(offlineContentOperations.loadDownloadRequests()).thenReturn(buildDownloadRequestObservable(downloadRequest2, createDownloadRequest(678)));
        when(downloadHandler.isDownloading()).thenReturn(true);

        startService();

        verify(notificationController).onPendingRequests(3);
    }

    @Test
    public void updatesNotificationWhenLikedTrackDownloaded() {
        service.onSuccess(downloadResult1);

        verify(notificationController).onDownloadSuccess();
    }

    @Test
    public void showsNotificationWhenAllLikedTrackDownloaded() {
        service.onSuccess(downloadResult1);

        verify(notificationController).onDownloadsFinished();
    }

    @Test
    public void startServiceWithDownloadActionDoesNotCreateNotificationWhenNoPendingDownloadsExists() {
        when(offlineContentOperations.loadDownloadRequests()).thenReturn(Observable.<List<DownloadRequest>>empty());

        startService();

        verify(notificationController, never()).onPendingRequests(anyInt());
    }

    @Test
    public void startServiceWithCancelDownloadActionStopRequestProcessing() {
        PublishSubject<List<DownloadRequest>> pendingRequestsSubject = PublishSubject.create();
        when(offlineContentOperations.loadDownloadRequests()).thenReturn(pendingRequestsSubject);

        startService();
        stopService();

        pendingRequestsSubject.onNext(Arrays.asList(downloadRequest1, downloadRequest2));
        verify(notificationController, never()).onPendingRequests(anyInt());
        verify(downloadHandler).quit();
    }

    @Test
    public void startServiceWithDownloadActionCancelsAnyExistingRetryScheduling() {
        when(offlineContentOperations.loadDownloadRequests()).thenReturn(Observable.<List<DownloadRequest>>empty());

        startService();

        verify(offlineContentScheduler).cancelPendingRetries();
    }

    private int startService() {
        Intent intent = new Intent(Robolectric.application, OfflineContentService.class);
        intent.setAction(OfflineContentService.ACTION_START_DOWNLOAD);
        return service.onStartCommand(intent, 0, 0);
    }

    private int stopService() {
        Intent intent = new Intent(Robolectric.application, OfflineContentService.class);
        intent.setAction(OfflineContentService.ACTION_STOP_DOWNLOAD);
        return service.onStartCommand(intent, 0, 0);
    }

    private DownloadRequest createDownloadRequest(long id) {
        return new DownloadRequest(Urn.forTrack(id), "http://" + id);
    }

    private Observable<List<DownloadRequest>> buildDownloadRequestObservable(DownloadRequest... downloadRequests) {
        final List<DownloadRequest> requestsList = new ArrayList<>(Arrays.asList(downloadRequests));
        return Observable.just(requestsList);
    }

    private void expectDownloadStarted(EntityStateChangedEvent event, Urn urn) {
        expect(event.getKind()).toBe(EntityStateChangedEvent.DOWNLOAD);
        expect(event.getNextChangeSet().get(TrackProperty.OFFLINE_DOWNLOADING)).toBeTrue();
        expect(event.getChangeMap().keySet()).toContainExactly(urn);
    }

    private void expectDownloadFinished(EntityStateChangedEvent event, Urn urn) {
        expect(event.getKind()).toBe(EntityStateChangedEvent.DOWNLOAD);
        expect(event.getNextChangeSet().get(TrackProperty.OFFLINE_DOWNLOADING)).toBeFalse();
        expect(event.getNextChangeSet().contains(TrackProperty.OFFLINE_DOWNLOADED_AT)).toBeTrue();
        expect(event.getChangeMap().keySet()).toContainExactly(urn);
    }

    private void expectDownloadFailed(EntityStateChangedEvent event, Urn urn) {
        expect(event.getKind()).toBe(EntityStateChangedEvent.DOWNLOAD);
        expect(event.getNextChangeSet().get(TrackProperty.OFFLINE_DOWNLOADING)).toBeFalse();
        expect(event.getNextChangeSet().contains(TrackProperty.OFFLINE_UNAVAILABLE_AT)).toBeTrue();
        expect(event.getChangeMap().keySet()).toContainExactly(urn);
    }

    private void expectDownloadsPending(EntityStateChangedEvent event, Urn... urns) {
        expect(event.getKind()).toBe(EntityStateChangedEvent.DOWNLOAD);

        expect(event.getChangeMap().keySet()).toContainExactly(urns);
        for (Urn urn : urns) {
            final PropertySet changeSet = event.getChangeMap().get(urn);
            expect(changeSet.get(TrackProperty.OFFLINE_DOWNLOADING)).toBeFalse();
            expect(changeSet.contains(TrackProperty.OFFLINE_REQUESTED_AT)).toBeTrue();
        }
    }

    private void expectDownloadsRemoved(EntityStateChangedEvent event, Urn... urns) {
        expect(event.getKind()).toBe(EntityStateChangedEvent.DOWNLOAD);
        expect(event.getChangeMap().keySet()).toContainExactly(urns);
        for (Urn urn : urns) {
            final PropertySet changeSet = event.getChangeMap().get(urn);
            expect(changeSet.get(TrackProperty.OFFLINE_DOWNLOADING)).toBeFalse();
            expect(changeSet.contains(TrackProperty.OFFLINE_REMOVED_AT)).toBeTrue();
        }
    }

}