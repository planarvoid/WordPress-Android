package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineContentEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.rx.eventbus.TestEventBus;
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
    private TestObservables.MockObservable<List<Urn>> deletePendingRemoval;

    private OfflineContentService service;
    private DownloadRequest downloadRequest1;
    private DownloadRequest downloadRequest2;
    private DownloadResult downloadResult1;
    private TestEventBus eventBus;
    private Message downloadMessage;

    @Before
    public void setUp() {
        deletePendingRemoval = TestObservables.emptyObservable();
        downloadRequest1 = createDownloadRequest(123L);
        downloadRequest2 = createDownloadRequest(456L);
        downloadResult1 = DownloadResult.success(Urn.forTrack(123L));
        eventBus = new TestEventBus();

        service = new OfflineContentService(downloadOperations, offlineContentOperations, notificationController,
                eventBus, offlineContentScheduler, handlerFactory, Schedulers.immediate());

        when(downloadOperations.deletePendingRemovals()).thenReturn(deletePendingRemoval);
        when(handlerFactory.create(service)).thenReturn(downloadHandler);
        downloadMessage = new Message();
        when(downloadHandler.obtainMessage(eq(DownloadHandler.ACTION_DOWNLOAD), any(Object.class))).thenReturn(downloadMessage);

        service.onCreate();
    }

    @Test
    public void deletePendingRemovalsWhenStarting() {
        when(offlineContentOperations.updateDownloadRequestsFromLikes()).thenReturn(buildDownloadRequestObservable(downloadRequest1));

        startService();

        expect(deletePendingRemoval.subscribedTo()).toBeTrue();
    }

    @Test
    public void startsSyncingWhenALikedTrackIsNotSynced() {
        when(offlineContentOperations.updateDownloadRequestsFromLikes()).thenReturn(buildDownloadRequestObservable(downloadRequest1));

        startService();

        verify(downloadHandler).sendMessage(downloadMessage);
    }

    @Test
    public void doesNotStartSyncingWhenAllLikedTracksAreSynced() {
        when(offlineContentOperations.updateDownloadRequestsFromLikes()).thenReturn(Observable.<List<DownloadRequest>>empty());

        startService();

        verify(downloadHandler, never()).handleMessage(any(Message.class));
    }

    @Test
    public void sendsDownloadStartedEventWhenDownloadingLikedTrack() {
        when(offlineContentOperations.updateDownloadRequestsFromLikes()).thenReturn(buildDownloadRequestObservable(downloadRequest1));

        startService();

        List<OfflineContentEvent> offlineContentEvents = eventBus.eventsOn(EventQueue.OFFLINE_CONTENT);
        expect(offlineContentEvents).toContainExactly(OfflineContentEvent.idle(), OfflineContentEvent.queueUpdate(), OfflineContentEvent.start());

        expect(eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED).getKind()).toEqual(EntityStateChangedEvent.DOWNLOAD_STARTED);
    }

    @Test
    public void sendsStartAndStopEventIfServiceRunsButHasNoTracksToDownload() {
        when(offlineContentOperations.updateDownloadRequestsFromLikes()).thenReturn(Observable.just(Collections.<DownloadRequest>emptyList()));

        startService();

        List<OfflineContentEvent> offlineContentEvents = eventBus.eventsOn(EventQueue.OFFLINE_CONTENT);
        expect(offlineContentEvents).toContainExactly(OfflineContentEvent.idle(), OfflineContentEvent.queueUpdate(), OfflineContentEvent.start(), OfflineContentEvent.stop());
    }

    @Test
    public void cancelServiceEmitsOfflineSyncStopEvent() {
        stopService();

        List<OfflineContentEvent> offlineContentEvents = eventBus.eventsOn(EventQueue.OFFLINE_CONTENT);
        expect(offlineContentEvents).toContainExactly(OfflineContentEvent.idle(), OfflineContentEvent.stop());
    }

    @Test
    public void sendsDownloadStartedEventWhenLikedTrackDownloadStarted() {
        when(offlineContentOperations.updateDownloadRequestsFromLikes()).thenReturn(buildDownloadRequestObservable(downloadRequest1));

        startService();

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        expect(event.getKind()).toBe(EntityStateChangedEvent.DOWNLOAD_STARTED);
        expect(event.getChangeMap().keySet()).toContainExactly(downloadRequest1.urn);
    }

    @Test
    public void sendsDownloadSucceededEventWhenLikedTrackDownloaded() {
        when(offlineContentOperations.updateDownloadRequestsFromLikes()).thenReturn(buildDownloadRequestObservable(downloadRequest1));

        startService();
        service.onSuccess(downloadResult1);

        final List<OfflineContentEvent> offlineContentEvents = eventBus.eventsOn(EventQueue.OFFLINE_CONTENT);
        expect(offlineContentEvents).toContainExactly(OfflineContentEvent.idle(), OfflineContentEvent.queueUpdate(), OfflineContentEvent.start(), OfflineContentEvent.stop());

        List<EntityStateChangedEvent> entityStateChangedEvents = eventBus.eventsOn(EventQueue.ENTITY_STATE_CHANGED);
        expect(entityStateChangedEvents.get(0).getKind()).toBe(EntityStateChangedEvent.DOWNLOAD_STARTED);
        expect(entityStateChangedEvents.get(1).getKind()).toBe(EntityStateChangedEvent.DOWNLOAD_FINISHED);
        expect(entityStateChangedEvents.get(1).getChangeMap().keySet()).toContainExactly(downloadRequest1.urn);

    }

    @Test
    public void sendsDownloadFailedEventWhenLikedTrackDownloadFailed() {
        when(offlineContentOperations.updateDownloadRequestsFromLikes()).thenReturn(buildDownloadRequestObservable(downloadRequest1));

        startService();
        service.onError(downloadResult1);

        final List<OfflineContentEvent> offlineContentEvents = eventBus.eventsOn(EventQueue.OFFLINE_CONTENT);
        expect(offlineContentEvents).toContainExactly(OfflineContentEvent.idle(), OfflineContentEvent.queueUpdate(), OfflineContentEvent.start(), OfflineContentEvent.stop());

        List<EntityStateChangedEvent> entityStateChangedEvents = eventBus.eventsOn(EventQueue.ENTITY_STATE_CHANGED);
        expect(entityStateChangedEvents.get(0).getKind()).toBe(EntityStateChangedEvent.DOWNLOAD_STARTED);
        expect(entityStateChangedEvents.get(1).getKind()).toBe(EntityStateChangedEvent.DOWNLOAD_FAILED);
        expect(entityStateChangedEvents.get(1).getChangeMap().keySet()).toContainExactly(downloadRequest1.urn);

    }

    @Test
    public void showsNotificationWhenDownloadingLikedTrack() {
        when(offlineContentOperations.updateDownloadRequestsFromLikes()).thenReturn(buildDownloadRequestObservable(downloadRequest1, downloadRequest2));

        startService();

        verify(notificationController).onPendingRequests(2);
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
    public void startServiceWithDownloadActionCancelsAnyExistingRetrySchedulings() {
        when(offlineContentOperations.updateDownloadRequestsFromLikes()).thenReturn(Observable.<List<DownloadRequest>>empty());

        startService();

        verify(offlineContentScheduler).cancelPendingRetries();
    }

    @Test
    public void startServiceWithDownloadActionDoesNotCreateNotificationWhenNoPendingDownloadsExists() {
        when(offlineContentOperations.updateDownloadRequestsFromLikes()).thenReturn(Observable.<List<DownloadRequest>>empty());

        startService();

        verify(notificationController, never()).onPendingRequests(anyInt());
    }

    @Test
    public void startServiceWithCancelDownloadActionStopRequestProcessing() {
        PublishSubject<List<DownloadRequest>> pendingRequestsSubject = PublishSubject.create();
        when(offlineContentOperations.updateDownloadRequestsFromLikes()).thenReturn(pendingRequestsSubject);

        startService();
        stopService();

        pendingRequestsSubject.onNext(Arrays.asList(downloadRequest1, downloadRequest2));
        verify(notificationController, never()).onPendingRequests(anyInt());
        verify(downloadHandler).quit();
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

    private Observable<List<DownloadRequest>> buildDownloadRequestObservable(DownloadRequest... downloadRequest) {
        return Observable.just(Arrays.asList(downloadRequest));
    }
}