package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.DownloadHandler.MainHandler;
import static com.soundcloud.android.offline.DownloadOperations.ConnectionState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadOperations.DownloadProgressListener;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.WriteResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.os.Message;

public class DownloadHandlerTest extends AndroidUnitTest {

    @Mock MainHandler mainHandler;
    @Mock DownloadOperations downloadOperations;
    @Mock TrackDownloadsStorage tracksStorage;
    @Mock SecureFileStorage secureFileStorage;
    @Mock WriteResult writeResult;

    private DownloadHandler handler;
    private Message successMessage;
    private Message failureMessage;
    private Message progressMessage;
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
        downloadRequest = ModelFixtures.downloadRequestFromLikes(Urn.forTrack(123L));
        successResult = DownloadState.success(downloadRequest);
        failedResult = DownloadState.connectionError(downloadRequest, ConnectionState.NOT_ALLOWED);
        unavailableResult = DownloadState.unavailable(downloadRequest);
        notEnoughSpaceResult = DownloadState.notEnoughSpace(downloadRequest);
        cancelledResult = DownloadState.canceled(downloadRequest);

        successMessage = createMessage(downloadRequest);
        progressMessage = createMessage(downloadRequest);
        failureMessage = createMessage(downloadRequest);
        notEnoughSpaceMessage = createMessage(downloadRequest);
        cancelMessage = createMessage(downloadRequest);

        handler = new DownloadHandler(mainHandler, downloadOperations, secureFileStorage, tracksStorage);
        when(mainHandler.obtainMessage(MainHandler.ACTION_DOWNLOAD_SUCCESS, successResult)).thenReturn(successMessage);
        when(mainHandler.obtainMessage(MainHandler.ACTION_DOWNLOAD_FAILED, failedResult)).thenReturn(failureMessage);
        when(mainHandler.obtainMessage(MainHandler.ACTION_DOWNLOAD_FAILED, notEnoughSpaceResult))
                .thenReturn(notEnoughSpaceMessage);
        when(mainHandler.obtainMessage(MainHandler.ACTION_DOWNLOAD_CANCEL, cancelledResult)).thenReturn(cancelMessage);

        when(writeResult.success()).thenReturn(true);
        when(tracksStorage.storeCompletedDownload(any(DownloadState.class))).thenReturn(writeResult);
        when(tracksStorage.markTrackAsUnavailable(any(Urn.class))).thenReturn(writeResult);
    }

    @Test
    public void sendsProgressOfZeroWhenStarting() {
        when(mainHandler.obtainMessage(eq(MainHandler.ACTION_DOWNLOAD_PROGRESS), any())).thenReturn(progressMessage);
        when(downloadOperations.download(same(downloadRequest), any(DownloadProgressListener.class))).thenReturn(successResult);
        ArgumentCaptor<DownloadState> stateArgumentCaptor = ArgumentCaptor.forClass(DownloadState.class);

        handler.handleMessage(successMessage);

        verify(mainHandler).sendMessage(progressMessage);
        verify(mainHandler).obtainMessage(eq(MainHandler.ACTION_DOWNLOAD_PROGRESS), stateArgumentCaptor.capture());
        assertThat(stateArgumentCaptor.getValue().getProgress()).isEqualTo(0L);
    }

    @Test
    public void reportsProgressFromListener() {
        when(downloadOperations.download(same(downloadRequest), any(DownloadProgressListener.class))).thenReturn(successResult);
        ArgumentCaptor<DownloadState> stateArgumentCaptor = ArgumentCaptor.forClass(DownloadState.class);
        ArgumentCaptor<DownloadOperations.DownloadProgressListener> listenerArgumentCaptor = ArgumentCaptor.forClass(DownloadOperations.DownloadProgressListener.class);

        handler.handleMessage(successMessage);
        verify(downloadOperations).download(same(downloadRequest), listenerArgumentCaptor.capture());
        Mockito.reset(mainHandler);

        when(mainHandler.obtainMessage(eq(MainHandler.ACTION_DOWNLOAD_PROGRESS), any())).thenReturn(progressMessage);

        listenerArgumentCaptor.getValue().onProgress(123L);

        verify(mainHandler).sendMessage(progressMessage);
        verify(mainHandler).obtainMessage(eq(MainHandler.ACTION_DOWNLOAD_PROGRESS), stateArgumentCaptor.capture());
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

        verify(mainHandler).sendMessage(successMessage);
    }

    @Test
    public void sendsCancelMessageWhenDownloadWasCancelled() {
        when(downloadOperations.download(same(downloadRequest), any(DownloadProgressListener.class))).thenReturn(cancelledResult);

        handler.handleMessage(cancelMessage);

        verify(mainHandler).sendMessage(cancelMessage);
    }

    @Test
    public void sendsNotEnoughSpaceMessageWhenThereIsNotEnoughSpaceForTrack() {
        when(downloadOperations.download(same(downloadRequest), any(DownloadProgressListener.class))).thenReturn(notEnoughSpaceResult);

        handler.handleMessage(notEnoughSpaceMessage);

        verify(mainHandler).sendMessage(notEnoughSpaceMessage);
    }

    @Test
    public void sendsFailureMessageWhenDownloadFailed() {
        when(downloadOperations.download(same(downloadRequest), any(DownloadProgressListener.class))).thenReturn(failedResult);

        handler.handleMessage(failureMessage);

        verify(mainHandler).sendMessage(failureMessage);
    }

    @Test
    public void deletesFileWhenFailToStoreSuccessStatus() throws PropellerWriteException {
        when(downloadOperations.download(same(downloadRequest), any(DownloadProgressListener.class))).thenReturn(successResult);
        when(writeResult.success()).thenReturn(false);
        when(tracksStorage.storeCompletedDownload(successResult)).thenReturn(writeResult);

        handler.handleMessage(successMessage);

        verify(secureFileStorage).deleteTrack(downloadRequest.getTrack());
    }

    @Test
    public void markTrackAsUnavailable() {
        when(downloadOperations.download(same(downloadRequest), any(DownloadProgressListener.class))).thenReturn(unavailableResult);

        handler.handleMessage(successMessage);

        verify(tracksStorage).markTrackAsUnavailable(unavailableResult.getTrack());
    }

    private Message createMessage(DownloadRequest downloadRequest) {
        final Message msg = new Message();
        msg.obj = downloadRequest;
        return msg;
    }

}