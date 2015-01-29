package com.soundcloud.android.offline;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.content.Intent;
import rx.subjects.PublishSubject;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class OfflineContentServiceTest {

    @Mock private DownloadOperations downloadOperations;
    @Mock private DownloadNotificationController notificationController;

    private OfflineContentService service;
    private DownloadRequest downloadRequest1;
    private DownloadRequest downloadRequest2;
    private DownloadResult downloadResult1;

    @Before
    public void setUp() {
        downloadRequest1 = createDownloadRequest(123L);
        downloadRequest2 = createDownloadRequest(456L);
        downloadResult1 = new DownloadResult(true, Urn.forTrack(123L));
        service = new OfflineContentService(downloadOperations, notificationController);

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