package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.offline.DownloadHandler.MainHandler;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.StoreCompletedDownloadCommand;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.os.Message;

@RunWith(SoundCloudTestRunner.class)
public class DownloadHandlerTest {

    @Mock MainHandler mainHandler;
    @Mock DownloadOperations downloadOperations;
    @Mock StoreCompletedDownloadCommand completedDownloadCommand;
    private DownloadRequest downloadRequest;
    private DownloadResult downloadResult;

    private DownloadHandler handler;
    private Message successMessage;
    private Message failureMessage;

    @Before
    public void setUp() throws Exception {
        downloadRequest = new DownloadRequest(Urn.forTrack(123), "http://");
        downloadResult = new DownloadResult(Urn.forTrack(123));
        successMessage = createMessage(downloadRequest);
        failureMessage = createMessage(downloadRequest);

        handler = new DownloadHandler(mainHandler, downloadOperations, completedDownloadCommand);
        when(mainHandler.obtainMessage(MainHandler.ACTION_DOWNLOAD_SUCCESS, downloadResult)).thenReturn(successMessage);
        when(mainHandler.obtainMessage(MainHandler.ACTION_DOWNLOAD_FAILED, downloadRequest)).thenReturn(failureMessage);
    }

    @Test
    public void storesCompletedDownloadResult() throws DownloadFailedException {
        when(downloadOperations.download(downloadRequest)).thenReturn(downloadResult);

        handler.handleMessage(successMessage);

        expect(completedDownloadCommand.getInput()).toBe(downloadResult);
    }

    @Test
    public void doesNotStoreCompletedDownloadResultWhenDownloadFailed() throws Exception {
        when(downloadOperations.download(downloadRequest)).thenThrow(new DownloadFailedException(downloadRequest, null));

        handler.handleMessage(failureMessage);

        verifyZeroInteractions(completedDownloadCommand);
    }

    @Test
    public void sendsSuccessMessageWithDownloadResult() throws DownloadFailedException {
        when(downloadOperations.download(downloadRequest)).thenReturn(downloadResult);

        handler.handleMessage(successMessage);

        verify(mainHandler).sendMessage(successMessage);
    }

    @Test
    public void sendsFailureMessageWhenDownloadFailed() throws Exception {
        when(downloadOperations.download(downloadRequest)).thenThrow(new DownloadFailedException(downloadRequest, null));

        handler.handleMessage(failureMessage);

        verify(mainHandler).sendMessage(failureMessage);
    }

    private Message createMessage(DownloadRequest downloadRequest) {
        final Message msg = new Message();
        msg.obj = downloadRequest;
        return msg;
    }

}