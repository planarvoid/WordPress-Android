package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.offline.DownloadHandler.MainHandler;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.StoreCompletedDownloadCommand;
import com.soundcloud.android.offline.commands.UpdateContentAsUnavailableCommand;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.WriteResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.os.Message;

@RunWith(SoundCloudTestRunner.class)
public class DownloadHandlerTest {

    @Mock MainHandler mainHandler;
    @Mock DownloadOperations downloadOperations;
    @Mock StoreCompletedDownloadCommand completedDownloadCommand;
    @Mock UpdateContentAsUnavailableCommand updateContentAsUnavailable;

    private DownloadHandler handler;
    private Message successMessage;
    private Message failureMessage;
    private DownloadRequest downloadRequest;
    private DownloadResult downloadResultSuccess;
    private DownloadResult downloadResultFailed;
    private DownloadResult downloadResultUnavailable;

    @Before
    public void setUp() throws Exception {
        downloadRequest = new DownloadRequest(Urn.forTrack(123), "http://");
        downloadResultSuccess = DownloadResult.success(downloadRequest);
        downloadResultFailed = DownloadResult.failed(downloadRequest);
        downloadResultUnavailable = DownloadResult.unavailable(downloadRequest);

        successMessage = createMessage(downloadRequest);
        failureMessage = createMessage(downloadRequest);

        handler = new DownloadHandler(mainHandler, downloadOperations, completedDownloadCommand, updateContentAsUnavailable);
        when(mainHandler.obtainMessage(MainHandler.ACTION_DOWNLOAD_SUCCESS, downloadResultSuccess)).thenReturn(successMessage);
        when(mainHandler.obtainMessage(MainHandler.ACTION_DOWNLOAD_FAILED, downloadResultFailed)).thenReturn(failureMessage);
        when(updateContentAsUnavailable.toObservable()).thenReturn(Observable.<WriteResult>empty());
    }

    @Test
    public void storesCompletedDownloadResult() {
        when(downloadOperations.download(downloadRequest)).thenReturn(downloadResultSuccess);

        handler.handleMessage(successMessage);

        expect(completedDownloadCommand.getInput()).toBe(downloadResultSuccess);
    }

    @Test
    public void doesNotStoreCompletedDownloadResultWhenDownloadFailed() {
        when(downloadOperations.download(downloadRequest)).thenReturn(downloadResultFailed);

        handler.handleMessage(failureMessage);

        verifyZeroInteractions(completedDownloadCommand);
    }

    @Test
    public void sendsSuccessMessageWithDownloadResult() {
        when(downloadOperations.download(downloadRequest)).thenReturn(downloadResultSuccess);

        handler.handleMessage(successMessage);

        verify(mainHandler).sendMessage(successMessage);
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
        when(completedDownloadCommand.call()).thenThrow(new PropellerWriteException("Test", new Exception()));

        handler.handleMessage(successMessage);

        verify(downloadOperations).deleteTrack(downloadRequest.track);
    }

    @Test
    public void markTrackAsUnavailable() {
        when(downloadOperations.download(downloadRequest)).thenReturn(downloadResultUnavailable);

        handler.handleMessage(successMessage);

        expect(updateContentAsUnavailable.getInput()).toEqual(downloadResultUnavailable.getTrack());
    }

    private Message createMessage(DownloadRequest downloadRequest) {
        final Message msg = new Message();
        msg.obj = downloadRequest;
        return msg;
    }

}