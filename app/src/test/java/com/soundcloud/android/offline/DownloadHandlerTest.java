package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.DownloadHandler.MainHandler;
import static com.soundcloud.android.offline.DownloadOperations.ConnectionState;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.WriteResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.os.Message;

public class DownloadHandlerTest extends AndroidUnitTest {

    @Mock MainHandler mainHandler;
    @Mock DownloadOperations downloadOperations;
    @Mock OfflineTracksStorage tracksStorage;
    @Mock SecureFileStorage secureFileStorage;
    @Mock WriteResult writeResult;

    private DownloadHandler handler;
    private Message successMessage;
    private Message failureMessage;
    private Message notEnoughSpaceMessage;
    private Message cancelMessage;
    private DownloadRequest downloadRequest;

    private DownloadResult successResult;
    private DownloadResult failedResult;
    private DownloadResult cancelledResult;
    private DownloadResult unavailableResult;
    private DownloadResult notEnoughSpaceResult;

    @Before
    public void setUp() throws Exception {
        downloadRequest = new DownloadRequest(Urn.forTrack(123), 12345);
        successResult = DownloadResult.success(downloadRequest);
        failedResult = DownloadResult.connectionError(downloadRequest, ConnectionState.NOT_ALLOWED);
        unavailableResult = DownloadResult.unavailable(downloadRequest);
        notEnoughSpaceResult = DownloadResult.notEnoughSpace(downloadRequest);
        cancelledResult = DownloadResult.canceled(downloadRequest);

        successMessage = createMessage(downloadRequest);
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
        when(tracksStorage.storeCompletedDownload(any(DownloadResult.class))).thenReturn(writeResult);
        when(tracksStorage.markTrackAsUnavailable(any(Urn.class))).thenReturn(writeResult);
    }

    @Test
    public void storesCompletedDownloadResult() {
        when(downloadOperations.download(downloadRequest)).thenReturn(successResult);

        handler.handleMessage(successMessage);

        verify(tracksStorage).storeCompletedDownload(successResult);
    }

    @Test
    public void doesNotStoreCompletedDownloadResultWhenDownloadFailed() {
        when(downloadOperations.download(downloadRequest)).thenReturn(failedResult);

        handler.handleMessage(failureMessage);

        verify(tracksStorage, never()).storeCompletedDownload(failedResult);
    }

    @Test
    public void sendsSuccessMessageWithDownloadResult() {
        when(downloadOperations.download(downloadRequest)).thenReturn(successResult);

        handler.handleMessage(successMessage);

        verify(mainHandler).sendMessage(successMessage);
    }

    @Test
    public void sendsCancelMessageWhenDownloadWasCancelled() {
        when(downloadOperations.download(downloadRequest)).thenReturn(cancelledResult);

        handler.handleMessage(cancelMessage);

        verify(mainHandler).sendMessage(cancelMessage);
    }

    @Test
    public void sendsNotEnoughSpaceMessageWhenThereIsNotEnoughSpaceForTrack() {
        when(downloadOperations.download(downloadRequest)).thenReturn(notEnoughSpaceResult);

        handler.handleMessage(notEnoughSpaceMessage);

        verify(mainHandler).sendMessage(notEnoughSpaceMessage);
    }

    @Test
    public void sendsFailureMessageWhenDownloadFailed() {
        when(downloadOperations.download(downloadRequest)).thenReturn(failedResult);

        handler.handleMessage(failureMessage);

        verify(mainHandler).sendMessage(failureMessage);
    }

    @Test
    public void deletesFileWhenFailToStoreSuccessStatus() throws PropellerWriteException {
        when(downloadOperations.download(downloadRequest)).thenReturn(successResult);
        when(writeResult.success()).thenReturn(false);
        when(tracksStorage.storeCompletedDownload(successResult)).thenReturn(writeResult);

        handler.handleMessage(successMessage);

        verify(secureFileStorage).deleteTrack(downloadRequest.track);
    }

    @Test
    public void markTrackAsUnavailable() {
        when(downloadOperations.download(downloadRequest)).thenReturn(unavailableResult);

        handler.handleMessage(successMessage);

        verify(tracksStorage).markTrackAsUnavailable(unavailableResult.getTrack());
    }

    private Message createMessage(DownloadRequest downloadRequest) {
        final Message msg = new Message();
        msg.obj = downloadRequest;
        return msg;
    }

}