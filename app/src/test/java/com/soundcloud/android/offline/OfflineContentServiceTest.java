package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.DownloadOperations.ConnectionState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.app.Notification;
import android.content.Intent;
import android.os.Message;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OfflineContentServiceTest extends AndroidUnitTest {

    @Mock private DownloadOperations downloadOperations;
    @Mock private DownloadNotificationController notificationController;
    @Mock private OfflineContentScheduler offlineContentScheduler;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private DownloadHandler.Builder handlerFactory;
    @Mock private DownloadHandler downloadHandler;
    @Mock private Notification notification;
    @Mock private OfflineStatePublisher publisher;

    private static final Urn TRACK_1 = Urn.forTrack(123L);
    private static final Urn TRACK_2 = Urn.forTrack(456L);
    private final DownloadRequest downloadRequest1 = createDownloadRequest(TRACK_1);
    private final DownloadRequest downloadRequest2 = createDownloadRequest(Urn.forTrack(456L));
    private final DownloadState downloadState1 = DownloadState.success(downloadRequest1);
    private final DownloadState unavailableTrackResult1 = DownloadState.unavailable(downloadRequest1);
    private final DownloadState failedResult1 =
            DownloadState.connectionError(downloadRequest1, ConnectionState.NOT_ALLOWED);

    private Observable<List<Urn>> deletePendingRemoval;
    private OfflineContentService service;
    private Message downloadMessage;
    private DownloadQueue downloadQueue;

    @Before
    public void setUp() {
        downloadMessage = new Message();
        deletePendingRemoval = Observable.empty();
        downloadQueue = new DownloadQueue();

        when(downloadHandler.getCurrentTrack()).thenReturn(Urn.NOT_SET);
        when(downloadHandler.obtainMessage(eq(DownloadHandler.ACTION_DOWNLOAD), any(Object.class)))
                .thenReturn(downloadMessage);
        when(offlineContentOperations.loadContentToDelete()).thenReturn(deletePendingRemoval);
        when(offlineContentOperations.loadOfflineContentUpdates())
                .thenReturn(Observable.<OfflineContentRequests>never());
        when(notificationController.onPendingRequests(any(DownloadQueue.class))).thenReturn(notification);

        service = new OfflineContentService(downloadOperations, offlineContentOperations, notificationController,
                offlineContentScheduler, handlerFactory, publisher, downloadQueue);
        when(handlerFactory.create(service)).thenReturn(downloadHandler);
        service.onCreate();
    }

    @Test
    public void resetsDownloadQueueWhenStartingAService() {
        final OfflineContentRequests updates = new OfflineContentRequests(
                Arrays.asList(downloadRequest1, downloadRequest2),
                Arrays.asList(downloadRequest1),
                Collections.<DownloadRequest>emptyList(),
                Collections.<Urn>emptyList()
        );

        when(offlineContentOperations.loadOfflineContentUpdates()).thenReturn(Observable.just(updates));
        startService();

        assertThat(downloadQueue.getRequests()).contains(downloadRequest2);
    }

    @Test
    public void publishesNotDownloadableStateChangesWhenStartingAService() {
        final OfflineContentRequests updates = new OfflineContentRequests(
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Arrays.asList(downloadRequest1),
                Collections.<Urn>emptyList()
        );

        when(offlineContentOperations.loadOfflineContentUpdates()).thenReturn(Observable.just(updates));
        startService();

        verify(publisher).publishNotDownloadableStateChanges(downloadQueue, updates, Urn.NOT_SET);
    }

    @Test
    public void deletePendingRemovalsWhenStarting() {
        final List<Urn> tracksToBeRemoved = Arrays.asList(TRACK_1, TRACK_2);
        deletePendingRemoval = Observable.just(tracksToBeRemoved);

        when(offlineContentOperations.loadContentToDelete()).thenReturn(deletePendingRemoval);
        startService();

        verify(downloadOperations).removeOfflineTracks(tracksToBeRemoved);
    }

    @Test
    public void startsDownloadingDownloadRequests() {
        setUpSingleDownload();
        startService();

        verify(downloadHandler).sendMessage(downloadMessage);
    }

    @Test
    public void doesNotStartSyncingWhenNoDownloadRequest() {
        setupNoDownloadRequest();

        startService();

        verify(downloadHandler, never()).handleMessage(any(Message.class));
    }

    @Test
    public void publishesDownloadRequestedWhenCreatingRequestsQueue() {
        final OfflineContentRequests updates = new OfflineContentRequests(
                Arrays.asList(downloadRequest1, downloadRequest2),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<Urn>emptyList()
        );
        when(offlineContentOperations.loadOfflineContentUpdates()).thenReturn(Observable.just(updates));
        startService();

        verify(publisher).publishDownloadsRequested(downloadQueue);
    }

    @Test
    public void publishedDownloadingWhenDownloadStarts() {
        setUpSingleDownload();
        startService();

        verify(publisher).publishDownloading(downloadRequest1);
    }

    @Test
    public void publishesDownloadSuccessEventWhenTrackDownloadSucceeded() {
        startService();
        service.onSuccess(downloadState1);

        verify(publisher).publishDownloadSuccessfulEvents(downloadQueue, downloadState1);
    }

    @Test
    public void publishesDownloadErrorEventsEventWhenTrackDownloadFailed() {
        startService();
        service.onError(downloadState1);

        verify(publisher).publishDownloadErrorEvents(downloadQueue, downloadState1);
    }

    @Test
    public void publishesDownloadCancelEventsWhenTrackDownloadWasCancelled() {
        startService();
        service.onCancel(downloadState1);

        verify(publisher).publishDownloadCancelEvents(downloadQueue, downloadState1);
    }

    @Test
    public void stopAndScheduleRetryWhenTrackDownloadFailed() {
        setUpSingleDownload();

        startService();
        service.onError(failedResult1);

        verify(offlineContentScheduler).scheduleRetry();
        verify(downloadHandler).quit();
    }

    @Test
    public void connectionErrorNotificationWhenTrackDownloadFailed() {
        setUpSingleDownload();

        startService();
        service.onError(failedResult1);

        verify(notificationController).onConnectionError(failedResult1);
    }

    @Test
    public void stopAndScheduleRetryWhenNoValidNetworkOnNewDownload() {
        setUpsDownloads(downloadRequest1, downloadRequest2);

        startService();
        service.onError(failedResult1);

        verify(offlineContentScheduler).scheduleRetry();
        verify(downloadHandler).quit();
    }

    @Test
    public void continueDownloadNextTrackWhenTrackUnavailableForDownload() {
        setUpSingleDownload();

        startService();
        service.onError(unavailableTrackResult1);

        verify(notificationController).onDownloadError(unavailableTrackResult1);
        verify(downloadHandler).sendMessage(downloadMessage);
    }


    @Test
    public void showsNotificationWhenDownloading() {
        setUpsDownloads(downloadRequest1, downloadRequest2);

        startService();

        verify(notificationController).onPendingRequests(downloadQueue);
    }

    @Test
    public void updatesNotificationWhenAlreadyDownloading() {
        when(downloadHandler.isDownloading()).thenReturn(true);
        setUpsDownloads(downloadRequest1, downloadRequest2);

        startService();

        verify(notificationController).onPendingRequests(downloadQueue);
    }

    @Test
    public void republishDownloadingWhenRestartingService() {
        when(downloadHandler.isDownloading()).thenReturn(true);
        when(downloadHandler.getCurrentRequest()).thenReturn(downloadRequest1);
        setUpsDownloads(downloadRequest2);

        startService();

        verify(publisher).publishDownloading(downloadRequest1);
    }

    @Test
    public void updatesNotificationWhenTrackDownloaded() {
        service.onSuccess(downloadState1);

        verify(notificationController).onDownloadSuccess(downloadState1);
    }

    @Test
    public void showsNotificationWhenAllTrackDownloaded() {
        service.onSuccess(downloadState1);

        verify(notificationController).onDownloadsFinished(downloadState1);
    }

    @Test
    public void startServiceWithDownloadActionDoesNotCreateNotificationWhenNoPendingDownloadsExists() {
        setupNoDownloadRequest();

        startService();

        verify(notificationController, never()).onPendingRequests(any(DownloadQueue.class));
    }

    @Test
    public void startServiceWithCancelDownloadActionStopRequestProcessing() {
        final PublishSubject<OfflineContentRequests> observable = PublishSubject.create();
        when(offlineContentOperations.loadOfflineContentUpdates()).thenReturn(observable);

        startService();
        stopService();

        observable.onNext(createSingleDownloadRequestUpdate());

        verify(notificationController, never()).onPendingRequests(any(DownloadQueue.class));
        verify(downloadHandler).quit();
    }

    @Test
    public void startServiceWithDownloadActionCancelsAnyExistingRetryScheduling() {
        setUpSingleDownload();

        startService();

        verify(offlineContentScheduler).cancelPendingRetries();
    }

    @Test
    public void publishesDoneStateWhenStopingTheService() {
        startService();
        stopService();

        verify(publisher).publishDone();
    }

    private int startService() {
        Intent intent = new Intent(context(), OfflineContentService.class);
        intent.setAction(OfflineContentService.ACTION_START);
        return service.onStartCommand(intent, 0, 0);
    }

    private int stopService() {
        Intent intent = new Intent(context(), OfflineContentService.class);
        intent.setAction(OfflineContentService.ACTION_STOP);
        return service.onStartCommand(intent, 0, 0);
    }

    private DownloadRequest createDownloadRequest(Urn track) {
        return new DownloadRequest(track, 123456);
    }

    private void setupNoDownloadRequest() {
        final OfflineContentRequests updates = new OfflineContentRequests(
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<Urn>emptyList()
        );
        when(offlineContentOperations.loadOfflineContentUpdates()).thenReturn(Observable.just(updates));
    }

    private void setUpSingleDownload() {
        final OfflineContentRequests updates = createSingleDownloadRequestUpdate(downloadRequest1);
        when(offlineContentOperations.loadOfflineContentUpdates()).thenReturn(Observable.just(updates));
    }

    private void setUpsDownloads(DownloadRequest... requests) {
        final OfflineContentRequests updates = createSingleDownloadRequestUpdate(requests);
        when(offlineContentOperations.loadOfflineContentUpdates()).thenReturn(Observable.just(updates));
    }

    private OfflineContentRequests createSingleDownloadRequestUpdate(DownloadRequest... requests) {
        return new OfflineContentRequests(
                Arrays.asList(requests),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<Urn>emptyList()
        );
    }
}