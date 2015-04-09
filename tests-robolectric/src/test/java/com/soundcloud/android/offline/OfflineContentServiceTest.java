package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.events.EventQueue;
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

    private static final Urn TRACK_1 = Urn.forTrack(123L);
    private static final Urn TRACK_2 = Urn.forTrack(456L);
    private final DownloadRequest downloadRequest1 = createDownloadRequest(TRACK_1);
    private final DownloadRequest downloadRequest2 = createDownloadRequest(TRACK_2);
    private final DownloadResult downloadResult1 = DownloadResult.success(downloadRequest1);
    private final DownloadResult unavailableTrackResult1 = DownloadResult.unavailable(downloadRequest1);
    private final DownloadResult failedResult1 = DownloadResult.failed(downloadRequest1);

    private TestObservables.MockObservable<List<Urn>> deletePendingRemoval;
    private OfflineContentService service;
    private TestEventBus eventBus;
    private Message downloadMessage;

    @Before
    public void setUp() {
        deletePendingRemoval = TestObservables.emptyObservable();
        eventBus = new TestEventBus();
        service = new OfflineContentService(downloadOperations, offlineContentOperations, notificationController,
                eventBus, offlineContentScheduler, handlerFactory, new DownloadQueue());

        when(offlineContentOperations.loadContentToDelete()).thenReturn(deletePendingRemoval);
        when(handlerFactory.create(service)).thenReturn(downloadHandler);
        downloadMessage = new Message();
        when(downloadHandler.obtainMessage(eq(DownloadHandler.ACTION_DOWNLOAD), any(Object.class))).thenReturn(downloadMessage);
        when(downloadOperations.isValidNetwork()).thenReturn(true);
        when(offlineContentOperations.loadOfflineContentUpdates()).thenReturn(Observable.<OfflineContentRequests>never());

        service.onCreate();
    }

    @Test
    public void emitsNewDownlaodRequests() {
        final OfflineContentRequests updates = new OfflineContentRequests(
                Arrays.asList(downloadRequest1, downloadRequest2),
                Arrays.asList(downloadRequest1),
                Collections.<DownloadRequest>emptyList(),
                Collections.<Urn>emptyList()
        );

        when(offlineContentOperations.loadOfflineContentUpdates()).thenReturn(Observable.just(updates));
        startService();

        expect(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).toContainExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloadRequested(Arrays.asList(downloadRequest1, downloadRequest2)),
                CurrentDownloadEvent.downloading(downloadRequest1)
        );
    }

    @Test
    public void emitsNewTrackDownloadedWhenTrackIsRestoredFromPendingRemovals() {
        final OfflineContentRequests updates = new OfflineContentRequests(
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Arrays.asList(downloadRequest1),
                Collections.<Urn>emptyList()
        );

        when(offlineContentOperations.loadOfflineContentUpdates()).thenReturn(Observable.just(updates));
        startService();

        expect(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).toContainExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloaded(Arrays.asList(downloadRequest1))
        );
    }

    @Test
    public void emitsRemovedTrack() {
        final OfflineContentRequests updates = new OfflineContentRequests(
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Arrays.asList(TRACK_1));

        when(offlineContentOperations.loadOfflineContentUpdates()).thenReturn(Observable.just(updates));
        startService();

        expect(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).toContainExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloadRequestRemoved(Arrays.asList(downloadRequest1))
        );
    }

    @Test
    public void deletePendingRemovalsWhenStarting() {
        when(offlineContentOperations.loadContentToDelete()).thenReturn(deletePendingRemoval);

        startService();

        expect(deletePendingRemoval.subscribedTo()).toBeTrue();
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
    public void sendsDownloadPendingWhenCreatingRequestsQueue() {
        final OfflineContentRequests updates = new OfflineContentRequests(
                Arrays.asList(downloadRequest1, downloadRequest2),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<Urn>emptyList()
        );
        when(offlineContentOperations.loadOfflineContentUpdates()).thenReturn(Observable.just(updates));
        startService();

        expect(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).toContainExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloadRequested(Arrays.asList(downloadRequest1, downloadRequest2)),
                CurrentDownloadEvent.downloading(downloadRequest1)
        );
    }

    @Test
    public void sendsDownloadRemovedWhenUpdatingRequestsQueue() throws Exception {
        final OfflineContentRequests updates = new OfflineContentRequests(
                Arrays.asList(downloadRequest1, downloadRequest2),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<Urn>emptyList()
        );
        when(offlineContentOperations.loadOfflineContentUpdates()).thenReturn(Observable.just(updates));
        startService();

        when(downloadHandler.isCurrentRequest(downloadRequest1)).thenReturn(true);
        when(downloadHandler.isDownloading()).thenReturn(true);
        startService();

        expect(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).toContainExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloadRequested(Arrays.asList(downloadRequest1, downloadRequest2)),
                CurrentDownloadEvent.downloading(downloadRequest1),
                CurrentDownloadEvent.downloadRequestRemoved(Arrays.asList(downloadRequest2)),
                CurrentDownloadEvent.downloadRequested(Arrays.asList(downloadRequest2))
        );
    }

    @Test
    public void sendsDownloadStoppedEventWhenTrackDownloadSucceeded() {
        startService();
        service.onSuccess(downloadResult1);

        expect(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).toContainExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloaded(Arrays.asList(downloadRequest1))
        );
    }

    @Test
    public void sendsDownloadStoppedEventWhenTrackDownloadFailed() {
        startService();
        service.onError(downloadResult1);

        expect(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).toContainExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.unavailable(Arrays.asList(downloadRequest1))
        );
    }

    @Test
    public void sendsDownloadRequestEventForRelatedPlaylist() {
        List<Urn> relatedPlaylists = Arrays.asList(Urn.forPlaylist(123L), Urn.forPlaylist(456L));

        startService();
        service.onError(createFailedDownloadResult(TRACK_1, relatedPlaylists));

        expect(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).toContainExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.unavailable(false, Lists.newArrayList(TRACK_1)),
                CurrentDownloadEvent.downloadRequested(false, relatedPlaylists));
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

        verify(notificationController).onConnectionError();
    }

    @Test
    public void continueDownloadNextTrackWhenTrackUnavailableForDownload() {
        setUpSingleDownload();

        startService();
        service.onError(unavailableTrackResult1);

        verify(notificationController).onDownloadError();
        verify(downloadHandler).sendMessage(downloadMessage);
    }


    @Test
    public void stopAndScheduleRetryWhenNoValidNetworkOnNewDownload() {
        setUpsDownloads(downloadRequest1, downloadRequest2);

        startService();
        when(downloadOperations.isValidNetwork()).thenReturn(false);
        service.onSuccess(unavailableTrackResult1);

        verify(offlineContentScheduler).scheduleRetry();
        verify(downloadHandler).quit();
    }

    @Test
    public void showsNotificationWhenDownloading() {
        setUpsDownloads(downloadRequest1, downloadRequest2);

        startService();

        verify(notificationController).onPendingRequests(2);
    }

    @Test
    public void updatesNotificationWhenAlreadyDownloading() {
        when(downloadHandler.isDownloading()).thenReturn(true);
        setUpsDownloads(downloadRequest1, downloadRequest2);

        startService();

        verify(notificationController).onPendingRequests(3);
    }

    @Test
    public void updatesNotificationWhenTrackDownloaded() {
        service.onSuccess(downloadResult1);

        verify(notificationController).onDownloadSuccess();
    }

    @Test
    public void showsNotificationWhenAllTrackDownloaded() {
        service.onSuccess(downloadResult1);

        verify(notificationController).onDownloadsFinished();
    }

    @Test
    public void startServiceWithDownloadActionDoesNotCreateNotificationWhenNoPendingDownloadsExists() {
        setupNoDownloadRequest();

        startService();

        verify(notificationController, never()).onPendingRequests(anyInt());
    }

    @Test
    public void startServiceWithCancelDownloadActionStopRequestProcessing() {
        final PublishSubject<OfflineContentRequests> observable = PublishSubject.create();
        when(offlineContentOperations.loadOfflineContentUpdates()).thenReturn(observable);

        startService();
        stopService();

        observable.onNext(createSingleDownloadRequestUpdate());

        verify(notificationController, never()).onPendingRequests(anyInt());
        verify(downloadHandler).quit();
    }

    @Test
    public void startServiceWithDownloadActionCancelsAnyExistingRetryScheduling() {
        setUpSingleDownload();

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

    private DownloadRequest createDownloadRequest(Urn track) {
        return new DownloadRequest(track, "http://" + track.getNumericId(), 123456);
    }

    private DownloadResult createFailedDownloadResult(Urn downloadedTrack, List<Urn> relatedPlaylists) {
        DownloadRequest downloadRequest = new DownloadRequest(downloadedTrack, "http://" + downloadedTrack.getNumericId(), 123456, false, relatedPlaylists);
        return DownloadResult.failed(downloadRequest);
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