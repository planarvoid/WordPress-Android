package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineSyncEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.content.Intent;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class OfflineContentServiceTest {

    @Mock private DownloadOperations downloadOperations;
    @Mock private DownloadNotificationController notificationController;
    @Mock private OfflineContentScheduler offlineContentScheduler;
    @Mock private OfflineContentOperations offlineContentOperations;

    private OfflineContentService service;
    private DownloadRequest downloadRequest1;
    private DownloadRequest downloadRequest2;
    private DownloadResult downloadResult1;
    private TestEventBus eventBus;

    @Before
    public void setUp() {
        downloadRequest1 = createDownloadRequest(123L);
        downloadRequest2 = createDownloadRequest(456L);
        downloadResult1 = new DownloadResult(true, Urn.forTrack(123L));
        eventBus = new TestEventBus();
        service = new OfflineContentService(downloadOperations, offlineContentOperations, notificationController, eventBus, offlineContentScheduler);

        when(offlineContentOperations.processPendingRemovals()).thenReturn(Observable.<Void>empty());
        when(downloadOperations.pendingDownloads()).thenReturn(Observable.<List<DownloadRequest>>empty());
    }

    @Test
    public void startServiceWithDownloadActionGetsPendingDownloadsFromDownloadOperations() {
        service.onStartCommand(createStartDownloadIntent(), 0, 0);

        verify(downloadOperations).pendingDownloads();
    }

    @Test
    public void startServiceWithDownloadActionUpdatesNotificationAfterRequestsAdded() {
        final List<DownloadRequest> requests = listOfPendingDownloads();
        when(downloadOperations.pendingDownloads()).thenReturn(Observable.just(requests));

        service.onStartCommand(createStartDownloadIntent(), 0, 0);
        verify(notificationController).onNewPendingRequests(requests.size());
    }

    @Test
    public void startServiceWithDownloadActionCancelsAnyExistingRetrySchedulings() {
        when(downloadOperations.pendingDownloads()).thenReturn(Observable.<List<DownloadRequest>>empty());

        service.onStartCommand(createStartDownloadIntent(), 0, 0);
        verify(offlineContentScheduler).cancelPendingRetries();
    }

    @Test
    public void startServiceWithDownloadActionDoesNotCreateNotificationWhenNoPendingDownloadsExists() {
        when(downloadOperations.pendingDownloads()).thenReturn(Observable.<List<DownloadRequest>>empty());

        service.onStartCommand(createStartDownloadIntent(), 0, 0);
        verify(notificationController, never()).onNewPendingRequests(anyInt());
    }

    @Test
    public void startServiceWithDownloadActionProcessesRequestWhenPending() {
        final List<DownloadRequest> requests = Arrays.asList(downloadRequest1);
        when(downloadOperations.pendingDownloads()).thenReturn(Observable.just(requests));
        when(downloadOperations.processDownloadRequests(requests)).thenReturn(Observable.just(downloadResult1));

        service.onStartCommand(createStartDownloadIntent(), 0, 0);
        verify(downloadOperations).processDownloadRequests(requests);
    }

    @Test
    public void startServiceEmitsOfflineSyncStartEvent() {
        when(downloadOperations.pendingDownloads()).thenReturn(Observable.<List<DownloadRequest>>never());
        service.onStartCommand(createStartDownloadIntent(), 0, 0);

        List<OfflineSyncEvent> offlineSyncEvents = eventBus.eventsOn(EventQueue.OFFLINE_SYNC);
        expect(offlineSyncEvents.size()).toBe(2);
        expect(offlineSyncEvents.get(0).getKind()).toEqual(OfflineSyncEvent.IDLE);
        expect(offlineSyncEvents.get(1).getKind()).toEqual(OfflineSyncEvent.START);
    }

    @Test
    public void cancelServiceEmitsOfflineSyncStopEvent() {
        when(downloadOperations.pendingDownloads()).thenReturn(Observable.<List<DownloadRequest>>never());
        service.onStartCommand(createCancelDownloadIntent(), 0, 0);

        List<OfflineSyncEvent> offlineSyncEvents = eventBus.eventsOn(EventQueue.OFFLINE_SYNC);
        expect(offlineSyncEvents.size()).toBe(2);
        expect(offlineSyncEvents.get(0).getKind()).toEqual(OfflineSyncEvent.IDLE);
        expect(offlineSyncEvents.get(1).getKind()).toEqual(OfflineSyncEvent.STOP);
    }

    @Test
    public void downloadCompletionEmitsOfflineSyncStartAndStopEvents() {
        service.onStartCommand(createStartDownloadIntent(), 0, 0);

        List<OfflineSyncEvent> offlineSyncEvents = eventBus.eventsOn(EventQueue.OFFLINE_SYNC);
        expect(offlineSyncEvents.size()).toBe(3);
        expect(offlineSyncEvents.get(0).getKind()).toEqual(OfflineSyncEvent.IDLE);
        expect(offlineSyncEvents.get(1).getKind()).toEqual(OfflineSyncEvent.START);
        expect(offlineSyncEvents.get(2).getKind()).toEqual(OfflineSyncEvent.STOP);
    }

    @Test
    public void startServiceWithCancelDownloadActionStopRequestProcessing() {
        PublishSubject<List<DownloadRequest>> pendingRequestsSubject = PublishSubject.create();
        when(downloadOperations.pendingDownloads()).thenReturn(pendingRequestsSubject);

        service.onStartCommand(createStartDownloadIntent(), 0, 0);
        service.onStartCommand(createCancelDownloadIntent(), 0, 0);

        pendingRequestsSubject.onNext(Arrays.asList(downloadRequest1, downloadRequest2));
        verify(notificationController, never()).onNewPendingRequests(anyInt());
    }

    private List<DownloadRequest> listOfPendingDownloads() {
        return Arrays.asList(downloadRequest1, downloadRequest2);
    }

    private DownloadRequest createDownloadRequest(long id) {
        return new DownloadRequest(Urn.forTrack(id), "http://" + id);
    }

    private Intent createStartDownloadIntent() {
        Intent intent = new Intent(Robolectric.application, OfflineContentService.class);
        intent.setAction(OfflineContentService.ACTION_START_DOWNLOAD);
        return intent;
    }

    private Intent createCancelDownloadIntent() {
        Intent intent = new Intent(Robolectric.application, OfflineContentService.class);
        intent.setAction(OfflineContentService.ACTION_STOP_DOWNLOAD);
        return intent;
    }
}