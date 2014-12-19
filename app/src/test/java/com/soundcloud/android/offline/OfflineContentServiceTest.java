package com.soundcloud.android.offline;

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
    public void startServiceGetsPendingDownloadsFromDownloadOperations() {
        service.onStartCommand(createDownloadTracksIntent(), 0, 0);

        verify(downloadOperations).pendingDownloads();
    }

    @Test
    public void startServiceUpdatesNotificationAfterRequestsAdded() {
        final List<DownloadRequest> requests = listOfPendingDownloads();
        when(downloadOperations.pendingDownloads()).thenReturn(Observable.just(requests));

        service.onStartCommand(createDownloadTracksIntent(), 0, 0);
        verify(notificationController).onNewPendingRequests(requests.size());
    }

    @Test
    public void startServiceProcessesRequestWhenPending() {
        final List<DownloadRequest> requests = Arrays.asList(downloadRequest1);
        when(downloadOperations.pendingDownloads()).thenReturn(Observable.just(requests));
        when(downloadOperations.processDownloadRequests(requests)).thenReturn(Observable.just(downloadResult1));

        service.onStartCommand(createDownloadTracksIntent(), 0, 0);
        verify(downloadOperations).processDownloadRequests(requests);
    }


    private List<DownloadRequest> listOfPendingDownloads() {
        return Arrays.asList(downloadRequest1, downloadRequest2);
    }

    private DownloadRequest createDownloadRequest(long id) {
        return new DownloadRequest(Urn.forTrack(id), "http://" + id);
    }

    private Intent createDownloadTracksIntent() {
        Intent intent = new Intent(Robolectric.application, OfflineContentService.class);
        intent.setAction(OfflineContentService.ACTION_DOWNLOAD_TRACKS);
        return intent;
    }

}