package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.DownloadOperations.ConnectionState;
import static com.soundcloud.android.offline.OfflineContentUpdates.builder;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import android.app.Notification;
import android.content.Intent;
import android.os.Message;

import java.util.Arrays;
import java.util.Collection;
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
    private final DownloadRequest downloadRequest1 = ModelFixtures.downloadRequestFromLikes(TRACK_1);
    private final DownloadRequest downloadRequest2 = ModelFixtures.downloadRequestFromLikes(TRACK_2);
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
                .thenReturn(Observable.<OfflineContentUpdates>never());
        when(notificationController.onPendingRequests(any(DownloadQueue.class))).thenReturn(notification);
        service = new OfflineContentService(downloadOperations, offlineContentOperations, notificationController,
                offlineContentScheduler, handlerFactory, publisher, downloadQueue, Schedulers.immediate());
        when(handlerFactory.create(service)).thenReturn(downloadHandler);
        service.onCreate();
    }

    @Test
    public void resetsDownloadQueueWhenStartingAService() {
        setUpOfflineContentUpdates(builder()
                .tracksToDownload(Arrays.asList(downloadRequest1, downloadRequest2))
                .build());

        startService();

        assertThat(downloadQueue.getRequests()).contains(downloadRequest2);
    }

    @Test
    public void publishesNotDownloadableStateChangesWhenStartingAService() {
        final OfflineContentUpdates updates = builder()
                .tracksToRemove(singletonList(Urn.forTrack(1L)))
                .tracksToRestore(singletonList(Urn.forTrack(2L)))
                .unavailableTracks(singletonList(Urn.forTrack(3L)))
                .tracksToDownload(singletonList(downloadRequest1))
                .build();

        setUpOfflineContentUpdates(updates);

        startService();

        verify(publisher).publishRemoved(updates.tracksToRemove());
        verify(publisher).publishDownloaded(updates.tracksToRestore());
        verify(publisher).publishUnavailable(updates.unavailableTracks());
    }

    @Test
    public void publishTrackRequestedWhenStarting() {
        setUpOfflineContentUpdates(builder()
                .tracksToDownload(singletonList(downloadRequest1))
                .build());

        startService();

        verify(publisher).publishRequested(singletonList(downloadRequest1.getTrack()));
    }

    @Test
    public void republishDownloadingWhenARequestIsAlreadyDownloading() {
        setUpOfflineContentUpdates(builder()
                .tracksToDownload(Arrays.asList(downloadRequest1, downloadRequest2))
                .build());
        when(downloadHandler.isDownloading()).thenReturn(true);
        when(downloadHandler.getCurrentRequest()).thenReturn(downloadRequest1);
        when(downloadHandler.getCurrentTrack()).thenReturn(downloadRequest1.getTrack());

        startService();

        final InOrder inOrder = inOrder(publisher);
        inOrder.verify(publisher).publishRequested(singletonList(downloadRequest2.getTrack()));
        inOrder.verify(publisher).publishDownloading(downloadHandler.getCurrentTrack());
    }

    @Test
    public void deletePendingRemovalsWhenStarting() {
        final List<Urn> tracksToBeRemoved = Arrays.asList(TRACK_1, TRACK_2);
        deletePendingRemoval = Observable.just(tracksToBeRemoved);

        when(downloadOperations.removeOfflineTracks(tracksToBeRemoved)).thenReturn(Observable.<Collection<Urn>>empty());
        when(offlineContentOperations.loadContentToDelete()).thenReturn(deletePendingRemoval);
        startService();

        verify(downloadOperations).removeOfflineTracks(tracksToBeRemoved);
    }

    @Test
    public void startsDownloadingDownloadRequests() {
        setUpsDownloads(downloadRequest1);
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
    public void publishedDownloadingWhenDownloadStarts() {
        setUpsDownloads(downloadRequest1);
        startService();

        verify(publisher).publishDownloading(downloadRequest1.getTrack());
    }

    @Test
    public void publishesDownloadSuccessEventWhenTrackDownloadSucceeded() {
        startService();
        service.onSuccess(downloadState1);

        verify(publisher).publishDownloaded(downloadRequest1.getTrack());
    }

    @Test
    public void setsOfflineContentStateWhenDownloadSucceeded() {
        startService();
        service.onSuccess(downloadState1);

        verify(offlineContentOperations).setHasOfflineContent(true);
    }

    @Test
    public void publishesDownloadErrorEventsEventWhenTrackDownloadFailed() {
        startService();
        service.onError(unavailableTrackResult1);

        verify(publisher).publishUnavailable(downloadRequest1.getTrack());
    }

    @Test
    public void publishesDownloadCancelEventsWhenTrackDownloadWasCancelled() {
        startService();
        service.onCancel(downloadState1);

        verifyNoMoreInteractions(publisher);
    }

    @Test
    public void stopAndScheduleRetryWhenTrackDownloadFailed() {
        setUpsDownloads(downloadRequest1);

        startService();
        service.onError(failedResult1);

        verify(publisher).publishRequested(downloadRequest1.getTrack());
        verify(offlineContentScheduler).scheduleRetry();
        verify(downloadHandler).quit();
    }

    @Test
    public void connectionErrorNotificationWhenTrackDownloadFailed() {
        setUpsDownloads(downloadRequest1);

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
        setUpsDownloads(downloadRequest1);

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
        when(downloadHandler.getCurrentRequest()).thenReturn(downloadRequest1);
        setUpsDownloads(downloadRequest1, downloadRequest2);

        startService();

        verify(notificationController).onPendingRequests(downloadQueue);
    }

    @Test
    public void mutesNotificationWhenNoContentIsRequestedForDownload() {
        setUpsDownloads();

        startService();

        verify(notificationController).reset();
    }

    @Test
    public void cancelRequestWhenDownloadingATrackNotRequestedAnyMore() {
        when(downloadHandler.isDownloading()).thenReturn(true);
        when(downloadHandler.getCurrentRequest()).thenReturn(downloadRequest1);
        setUpsDownloads(downloadRequest2);

        startService();

        verify(downloadHandler).cancel();
    }
    
    @Test
    public void publishRemovedWhenDownloadingATrackNotRequestedAnyMore() {
        when(downloadHandler.isDownloading()).thenReturn(true);
        when(downloadHandler.getCurrentRequest()).thenReturn(downloadRequest1);
        setUpsDownloads(downloadRequest2);

        startService();

        verify(publisher).publishRemoved(downloadRequest1.getTrack());
    }

    @Test
    public void republishDownloadingWhenRestartingService() {
        when(downloadHandler.isDownloading()).thenReturn(true);
        when(downloadHandler.getCurrentTrack()).thenReturn(downloadRequest1.getTrack());
        when(downloadHandler.getCurrentRequest()).thenReturn(downloadRequest1);
        setUpsDownloads(downloadRequest1, downloadRequest2);

        startService();

        verify(publisher).publishDownloading(downloadRequest1.getTrack());
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
        final PublishSubject<OfflineContentUpdates> observable = PublishSubject.create();
        when(offlineContentOperations.loadOfflineContentUpdates()).thenReturn(observable);

        startService();
        stopService();

        observable.onNext(builder().build());

        verify(notificationController, never()).onPendingRequests(any(DownloadQueue.class));
        verify(downloadHandler).quit();
    }

    @Test
    public void startServiceWithDownloadActionCancelsAnyExistingRetryScheduling() {
        setUpsDownloads(downloadRequest1);

        startService();

        verify(offlineContentScheduler).cancelPendingRetries();
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

    private void setupNoDownloadRequest() {
        setUpOfflineContentUpdates(builder().build());
    }

    private void setUpsDownloads(DownloadRequest... requests) {
        ExpectedOfflineContent expectedOfflineContent = mock(ExpectedOfflineContent.class);
        when(expectedOfflineContent.isEmpty()).thenReturn(requests.length == 0);

        setUpOfflineContentUpdates(builder()
                .tracksToDownload(Arrays.asList(requests))
                .userExpectedOfflineContent(expectedOfflineContent).build());
    }

    private void setUpOfflineContentUpdates(OfflineContentUpdates updates) {
        when(offlineContentOperations.loadOfflineContentUpdates()).thenReturn(Observable.just(updates));
    }

}
