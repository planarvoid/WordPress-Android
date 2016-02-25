package com.soundcloud.android.offline;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.downloadRequestFromLikes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadHandler.Listener;
import com.soundcloud.android.offline.DownloadOperations.DownloadProgressListener;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.WriteResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.os.Message;

public class DownloadHandlerTest extends AndroidUnitTest {

    @Mock Listener listener;
    @Mock DownloadOperations downloadOperations;
    @Mock TrackDownloadsStorage tracksStorage;
    @Mock SecureFileStorage secureFileStorage;
    @Mock OfflinePerformanceTracker performanceTracker;
    @Mock WriteResult writeResult;

    private DownloadHandler handler;
    private Message successMessage;
    private Message failureMessage;
    private Message notEnoughSpaceMessage;
    private Message cancelMessage;
    private DownloadRequest downloadRequest;

    private DownloadState successResult;
    private DownloadState failedResult;
    private DownloadState cancelledResult;
    private DownloadState unavailableResult;
    private DownloadState notEnoughSpaceResult;

    @Before
    public void setUp() throws Exception {
        downloadRequest = downloadRequestFromLikes(Urn.forTrack(123L));
        successResult = DownloadState.success(downloadRequest);
        failedResult = DownloadState.invalidNetworkError(downloadRequest);
        unavailableResult = DownloadState.unavailable(downloadRequest);
        notEnoughSpaceResult = DownloadState.notEnoughSpace(downloadRequest);
        cancelledResult = DownloadState.canceled(downloadRequest);

        successMessage = createMessage(downloadRequest);
        failureMessage = createMessage(downloadRequest);
        notEnoughSpaceMessage = createMessage(downloadRequest);
        cancelMessage = createMessage(downloadRequest);

        handler = new DownloadHandler(listener, downloadOperations, secureFileStorage, tracksStorage, performanceTracker);

        when(writeResult.success()).thenReturn(true);
        when(tracksStorage.storeCompletedDownload(any(DownloadState.class))).thenReturn(writeResult);
        when(tracksStorage.markTrackAsUnavailable(any(Urn.class))).thenReturn(writeResult);
    }

    @Test
    public void sendsProgressOfZeroWhenStarting() {
        when(downloadOperations.download(same(downloadRequest), any(DownloadProgressListener.class))).thenReturn(successResult);
        ArgumentCaptor<DownloadState> stateArgumentCaptor = ArgumentCaptor.forClass(DownloadState.class);

        handler.handleMessage(successMessage);

        verify(listener).onProgress(stateArgumentCaptor.capture());
        assertThat(stateArgumentCaptor.getValue().getProgress()).isEqualTo(0L);
    }

    @Test
    public void reportsProgressFromListener() {
        when(downloadOperations.download(same(downloadRequest), any(DownloadProgressListener.class))).thenReturn(successResult);
        ArgumentCaptor<DownloadState> stateArgumentCaptor = ArgumentCaptor.forClass(DownloadState.class);
        ArgumentCaptor<DownloadOperations.DownloadProgressListener> listenerArgumentCaptor = ArgumentCaptor.forClass(DownloadOperations.DownloadProgressListener.class);

        handler.handleMessage(successMessage);
        verify(downloadOperations).download(same(downloadRequest), listenerArgumentCaptor.capture());
        Mockito.reset(listener);

        listenerArgumentCaptor.getValue().onProgress(123L);

        verify(listener).onProgress(stateArgumentCaptor.capture());
        assertThat(stateArgumentCaptor.getValue().getProgress()).isEqualTo(123L);
    }

    @Test
    public void storesCompletedDownloadResult() {
        when(downloadOperations.download(same(downloadRequest), any(DownloadProgressListener.class))).thenReturn(successResult);

        handler.handleMessage(successMessage);

        verify(tracksStorage).storeCompletedDownload(successResult);
    }

    @Test
    public void doesNotStoreCompletedDownloadResultWhenDownloadFailed() {
        when(downloadOperations.download(same(downloadRequest), any(DownloadProgressListener.class))).thenReturn(failedResult);

        handler.handleMessage(failureMessage);

        verify(tracksStorage, never()).storeCompletedDownload(failedResult);
    }

    @Test
    public void sendsSuccessMessageWithDownloadResult() {
        when(downloadOperations.download(same(downloadRequest), any(DownloadProgressListener.class))).thenReturn(successResult);

        handler.handleMessage(successMessage);

        verify(listener).onSuccess(successResult);
    }

    @Test
    public void sendsCancelMessageWhenDownloadWasCancelled() {
        when(downloadOperations.download(same(downloadRequest), any(DownloadProgressListener.class))).thenReturn(cancelledResult);

        handler.handleMessage(cancelMessage);

        verify(listener).onCancel(cancelledResult);
    }

    @Test
    public void sendsNotEnoughSpaceMessageWhenThereIsNotEnoughSpaceForTrack() {
        when(downloadOperations.download(same(downloadRequest), any(DownloadProgressListener.class))).thenReturn(notEnoughSpaceResult);

        handler.handleMessage(notEnoughSpaceMessage);

        verify(listener).onError(notEnoughSpaceResult);
    }

    @Test
    public void sendsFailureMessageWhenDownloadFailed() {
        when(downloadOperations.download(same(downloadRequest), any(DownloadProgressListener.class))).thenReturn(failedResult);

        handler.handleMessage(failureMessage);

        verify(listener).onError(failedResult);
    }

    @Test
    public void deletesFileAndReportsDownloadFailedWhenFailToStoreSuccessStatus() throws PropellerWriteException {
        ArgumentCaptor<DownloadState> downloadStateCaptor = ArgumentCaptor.forClass(DownloadState.class);

        when(downloadOperations.download(same(downloadRequest), any(DownloadProgressListener.class))).thenReturn(successResult);
        when(writeResult.success()).thenReturn(false);
        when(tracksStorage.storeCompletedDownload(successResult)).thenReturn(writeResult);

        handler.handleMessage(successMessage);

        verify(secureFileStorage).deleteTrack(downloadRequest.getTrack());
        verify(listener).onError(downloadStateCaptor.capture());

        DownloadState errorState = downloadStateCaptor.getValue();
        assertThat(errorState.isDownloadFailed()).isTrue();
        assertThat(errorState.request).isEqualTo(downloadRequest);
    }

    @Test
    public void markTrackAsUnavailable() {
        when(downloadOperations.download(same(downloadRequest), any(DownloadProgressListener.class))).thenReturn(unavailableResult);

        handler.handleMessage(successMessage);

        verify(tracksStorage).markTrackAsUnavailable(unavailableResult.getTrack());
    }

    @Test
    public void tracksDownloadStartedAndCompleted() {
        when(downloadOperations.download(same(downloadRequest), any(DownloadProgressListener.class))).thenReturn(successResult);

        handler.handleMessage(successMessage);

        InOrder inOrder = inOrder(performanceTracker);
        inOrder.verify(performanceTracker).downloadStarted(downloadRequest);
        inOrder.verify(performanceTracker).downloadComplete(successResult);
    }

    @Test
    public void tracksDownloadStartedAndCancelled() {
        when(downloadOperations.download(same(downloadRequest), any(DownloadProgressListener.class))).thenReturn(cancelledResult);

        handler.handleMessage(cancelMessage);

        InOrder inOrder = inOrder(performanceTracker);
        inOrder.verify(performanceTracker).downloadStarted(downloadRequest);
        inOrder.verify(performanceTracker).downloadCancelled(cancelledResult);
    }

    @Test
    public void tracksDownloadStartedAndFailed() {
        when(downloadOperations.download(same(downloadRequest), any(DownloadProgressListener.class))).thenReturn(failedResult);

        handler.handleMessage(failureMessage);

        InOrder inOrder = inOrder(performanceTracker);
        inOrder.verify(performanceTracker).downloadStarted(downloadRequest);
        inOrder.verify(performanceTracker).downloadFailed(failedResult);
    }

    private Message createMessage(DownloadRequest downloadRequest) {
        final Message msg = new Message();
        msg.obj = downloadRequest;
        return msg;
    }

}
