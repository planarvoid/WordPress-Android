package com.soundcloud.android.offline;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.DeletePendingRemovalCommand;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.io.IOException;
import java.io.InputStream;

@RunWith(SoundCloudTestRunner.class)
public class DownloadOperationsTest {

    @Mock private StrictSSLHttpClient httpClient;
    @Mock private SecureFileStorage fileStorage;
    @Mock private InputStream inputStream;
    @Mock private DeletePendingRemovalCommand deleteOfflineContent;

    private DownloadOperations operations;

    private final Urn trackUrn = Urn.forTrack(123L);
    private final String streamUrl = "http://stream1.url";
    private final DownloadRequest downloadRequest = new DownloadRequest(trackUrn, streamUrl);

    @Before
    public void setUp() throws Exception {
        operations = new DownloadOperations(httpClient, fileStorage, deleteOfflineContent);
    }

    @Test
    public void downloadCallsHttpClientWithGivenFileUrl() throws IOException, DownloadFailedException {
        operations.download(downloadRequest);

        verify(httpClient).downloadFile(downloadRequest.fileUrl);
    }

    @Test
    public void downloadWritesToFileStorage() throws IOException, EncryptionException, DownloadFailedException {
        when(httpClient.downloadFile(streamUrl)).thenReturn(inputStream);

        operations.download(downloadRequest);

        InOrder inOrder = inOrder(fileStorage, inputStream);
        inOrder.verify(fileStorage).storeTrack(trackUrn, inputStream);
        inOrder.verify(inputStream).close();
    }

    @Test(expected = DownloadFailedException.class)
    public void returnsDownloadExceptionWithTrackInfoWhenDownloadFailed() throws Exception {
        final IOException ioException = new IOException("Test IOException");
        when(httpClient.downloadFile(streamUrl)).thenThrow(ioException);

        operations.download(downloadRequest);
    }

    @Test(expected = DownloadFailedException.class)
    public void returnsDownloadExceptionWhenEncryptionFailed() throws Exception {
        final EncryptionException encryptionException = new EncryptionException("Test EncryptionException", null);
        when(httpClient.downloadFile(streamUrl)).thenReturn(inputStream);
        doThrow(encryptionException).when(fileStorage).storeTrack(trackUrn, inputStream);

        operations.download(downloadRequest);
    }

    @Test
    public void deletesFileFromFailedDownload() throws IOException, EncryptionException {
        final IOException ioException = new IOException("Test IOException");
        when(httpClient.downloadFile(streamUrl)).thenThrow(ioException);

        downloadAndSwallowException();

        verify(fileStorage).deleteTrack(trackUrn);
    }

    @Test
    public void deletesFileFromFailedEncryption() throws Exception {
        final EncryptionException encryptionException = new EncryptionException("Test EncryptionException", null);
        when(httpClient.downloadFile(streamUrl)).thenReturn(inputStream);
        doThrow(encryptionException).when(fileStorage).storeTrack(trackUrn, inputStream);

        downloadAndSwallowException();

        verify(fileStorage).deleteTrack(trackUrn);
    }

    private void downloadAndSwallowException() {
        try {
            operations.download(downloadRequest);
        } catch (DownloadFailedException e) {}
    }

}
