package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineContentEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

import android.content.Intent;
import android.os.Message;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class OfflineContentServiceTest {

    @Mock private DownloadOperations downloadOperations;
    @Mock private DownloadNotificationController notificationController;
    @Mock private OfflineContentScheduler offlineContentScheduler;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private DownloadHandler.Builder handlerFactory;
    @Mock private DownloadHandler downloadHandler;
    @Mock private Action1<Object> deletePendingRemoval;

    private OfflineContentService service;
    private DownloadRequest downloadRequest1;
    private DownloadRequest downloadRequest2;
    private DownloadResult downloadResult1;
    private TestEventBus eventBus;
    private Message downloadMessage;

    @Before
    public void setUp() {
        downloadRequest1 = createDownloadRequest(123L);
        downloadRequest2 = createDownloadRequest(456L);
        downloadResult1 = new DownloadResult(Urn.forTrack(123L));
        eventBus = new TestEventBus();

        service = new OfflineContentService(downloadOperations, offlineContentOperations, notificationController,
                eventBus, offlineContentScheduler, handlerFactory);

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

        verify(deletePendingRemoval).call(any());
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
        expect(offlineContentEvents.size()).toBe(4);
        expect(offlineContentEvents.get(0).getKind()).toEqual(OfflineContentEvent.IDLE);
        expect(offlineContentEvents.get(1).getKind()).toEqual(OfflineContentEvent.QUEUE_UPDATED);
        expect(offlineContentEvents.get(2).getKind()).toEqual(OfflineContentEvent.START);
        expect(offlineContentEvents.get(3).getKind()).toEqual(OfflineContentEvent.DOWNLOAD_STARTED);
    }

    @Test
    public void cancelServiceEmitsOfflineSyncStopEvent() {
        stopService();

        List<OfflineContentEvent> offlineContentEvents = eventBus.eventsOn(EventQueue.OFFLINE_CONTENT);
        expect(offlineContentEvents.size()).toBe(2);
        expect(offlineContentEvents.get(0).getKind()).toEqual(OfflineContentEvent.IDLE);
        expect(offlineContentEvents.get(1).getKind()).toEqual(OfflineContentEvent.STOP);
    }

    @Test
    public void sendsDownloadStartedEventWhenLikedTrackDownloadStarted() {
        when(offlineContentOperations.updateDownloadRequestsFromLikes()).thenReturn(buildDownloadRequestObservable(downloadRequest1));

        startService();

        final OfflineContentEvent offlineContentEvent = eventBus.lastEventOn(EventQueue.OFFLINE_CONTENT);
        expect(offlineContentEvent.getKind()).toBe(OfflineContentEvent.DOWNLOAD_STARTED);
        expect(offlineContentEvent.getUrn()).toEqual(downloadRequest1.urn);
    }

    @Test
    public void sendsDownloadSucceededEventWhenLikedTrackDownloaded() {
        when(offlineContentOperations.updateDownloadRequestsFromLikes()).thenReturn(buildDownloadRequestObservable(downloadRequest1));

        startService();
        service.onSuccess(downloadResult1);

        final List<OfflineContentEvent> events = eventBus.eventsOn(EventQueue.OFFLINE_CONTENT);
        expect(events.get(0).getKind()).toBe(OfflineContentEvent.IDLE);
        expect(events.get(1).getKind()).toEqual(OfflineContentEvent.QUEUE_UPDATED);
        expect(events.get(2).getKind()).toBe(OfflineContentEvent.START);
        expect(events.get(3).getKind()).toBe(OfflineContentEvent.DOWNLOAD_STARTED);
        expect(events.get(4).getKind()).toBe(OfflineContentEvent.DOWNLOAD_FINISHED);
        expect(events.get(4).getUrn()).toEqual(downloadRequest1.urn);
        expect(events.get(5).getKind()).toBe(OfflineContentEvent.STOP);
    }

    @Test
    public void sendsDownloadFailedEventWhenLikedTrackDownloadFailed() {
        when(offlineContentOperations.updateDownloadRequestsFromLikes()).thenReturn(buildDownloadRequestObservable(downloadRequest1));

        startService();
        service.onError(downloadRequest1);

        final List<OfflineContentEvent> events = eventBus.eventsOn(EventQueue.OFFLINE_CONTENT);
        expect(events.get(0).getKind()).toBe(OfflineContentEvent.IDLE);
        expect(events.get(1).getKind()).toEqual(OfflineContentEvent.QUEUE_UPDATED);
        expect(events.get(2).getKind()).toBe(OfflineContentEvent.START);
        expect(events.get(3).getKind()).toBe(OfflineContentEvent.DOWNLOAD_STARTED);
        expect(events.get(4).getKind()).toBe(OfflineContentEvent.DOWNLOAD_FAILED);
        expect(events.get(4).getUrn()).toEqual(downloadRequest1.urn);
        expect(events.get(5).getKind()).toBe(OfflineContentEvent.STOP);
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

        verify(notificationController).onProgressUpdate();
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