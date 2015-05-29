package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.DownloadHandler.MainHandler;
import static com.soundcloud.android.offline.DownloadOperations.ConnectionState;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.WriteResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.os.Message;

@RunWith(SoundCloudTestRunner.class)
public class DownloadHandlerTest {

    @Mock MainHandler mainHandler;
    @Mock DownloadOperations downloadOperations;
    @Mock OfflineTracksStorage tracksStorage;
    @Mock SecureFileStorage secureFileStorage;
    @Mock WriteResult writeResult;

    private DownloadHandler handler;
    private Message successMessage;
    private Message failureMessage;
    private Message notEnoughSpaceMessage;
    private DownloadRequest downloadRequest;
    private DownloadResult downloadResultSuccess;
    private DownloadResult downloadResultFailed;
    private DownloadResult downloadResultUnavailable;
    private DownloadResult downloadResultNotEnoughSpace;

    @Before
    public void setUp() throws Exception {
        downloadRequest = new DownloadRequest(Urn.forTrack(123), 12345);
        downloadResultSuccess = DownloadResult.success(downloadRequest);
        downloadResultFailed = DownloadResult.connectionError(downloadRequest, ConnectionState.NOT_ALLOWED);
        downloadResultUnavailable = DownloadResult.unavailable(downloadRequest);
        downloadResultNotEnoughSpace = DownloadResult.notEnoughSpace(downloadRequest);

        successMessage = createMessage(downloadRequest);
        failureMessage = createMessage(downloadRequest);
        notEnoughSpaceMessage = createMessage(downloadRequest);

        handler = new DownloadHandler(mainHandler, downloadOperations, secureFileStorage, tracksStorage);
        when(mainHandler.obtainMessage(MainHandler.ACTION_DOWNLOAD_SUCCESS, downloadResultSuccess)).thenReturn(successMessage);
        when(mainHandler.obtainMessage(MainHandler.ACTION_DOWNLOAD_FAILED, downloadResultFailed)).thenReturn(failureMessage);
        when(mainHandler.obtainMessage(MainHandler.ACTION_DOWNLOAD_FAILED, downloadResultNotEnoughSpace)).thenReturn(notEnoughSpaceMessage);
        when(writeResult.success()).thenReturn(true);
        when(tracksStorage.storeCompletedDownload(any(DownloadResult.class))).thenReturn(writeResult);
        when(tracksStorage.markTrackAsUnavailable(any(Urn.class))).thenReturn(writeResult);

    }

    @Test
    public void storesCompletedDownloadResult() {
        when(downloadOperations.download(downloadRequest)).thenReturn(downloadResultSuccess);

        handler.handleMessage(successMessage);

        verify(tracksStorage).storeCompletedDownload(downloadResultSuccess);
    }

    @Test
    public void doesNotStoreCompletedDownloadResultWhenDownloadFailed() {
        when(downloadOperations.download(downloadRequest)).thenReturn(downloadResultFailed);

        handler.handleMessage(failureMessage);

        verify(tracksStorage, never()).storeCompletedDownload(downloadResultFailed);
    }

    @Test
    public void sendsSuccessMessageWithDownloadResult() {
        when(downloadOperations.download(downloadRequest)).thenReturn(downloadResultSuccess);

        handler.handleMessage(successMessage);

        verify(mainHandler).sendMessage(successMessage);
    }

    @Test
    public void sendsNotEnoughSpaceMessageWhenThereIsNotEnoughSpaceForTrack() {
        when(downloadOperations.download(downloadRequest)).thenReturn(downloadResultNotEnoughSpace);

        handler.handleMessage(notEnoughSpaceMessage);

        verify(mainHandler).sendMessage(notEnoughSpaceMessage);
    }

    @Test
    public void sendsFailureMessageWhenDownloadFailed() {
        when(downloadOperations.download(downloadRequest)).thenReturn(downloadResultFailed);

        handler.handleMessage(failureMessage);

        verify(mainHandler).sendMessage(failureMessage);
    }

    @Test
    public void deletesFileWhenFailToStoreSuccessStatus() throws PropellerWriteException {
        when(downloadOperations.download(downloadRequest)).thenReturn(downloadResultSuccess);
        when(writeResult.success()).thenReturn(false);
        when(tracksStorage.storeCompletedDownload(downloadResultSuccess)).thenReturn(writeResult);

        handler.handleMessage(successMessage);

        verify(secureFileStorage).deleteTrack(downloadRequest.track);
    }

    @Test
    public void markTrackAsUnavailable() {
        when(downloadOperations.download(downloadRequest)).thenReturn(downloadResultUnavailable);

        handler.handleMessage(successMessage);

        verify(tracksStorage).markTrackAsUnavailable(downloadResultUnavailable.getTrack());
    }

    private Message createMessage(DownloadRequest downloadRequest) {
        final Message msg = new Message();
        msg.obj = downloadRequest;
        return msg;
    }

}