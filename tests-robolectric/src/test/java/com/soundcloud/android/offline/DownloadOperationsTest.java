package com.soundcloud.android.offline;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static com.soundcloud.android.offline.StrictSSLHttpClient.DownloadResponse;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.DeletePendingRemovalCommand;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.NetworkConnectionHelper;
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
    @Mock private DownloadResponse response;
    @Mock private DeletePendingRemovalCommand deleteOfflineContent;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private InputStream downloadStream;
    @Mock private NetworkConnectionHelper connectionHelper;
    @Mock private OfflineSettingsStorage offlineSettings;

    private DownloadOperations operations;

    private final Urn trackUrn = Urn.forTrack(123L);
    private final String streamUrl = "http://stream1.url";
    private final DownloadRequest downloadRequest = new DownloadRequest(trackUrn, streamUrl);

    @Before
    public void setUp() throws Exception {
        operations = new DownloadOperations(httpClient, fileStorage, deleteOfflineContent, playQueueManager,
                connectionHelper, offlineSettings);
        when(httpClient.downloadFile(streamUrl)).thenReturn(response);
        when(response.isFailure()).thenReturn(false);
        when(response.isUnavailable()).thenReturn(false);
        when(response.getInputStream()).thenReturn(downloadStream);
    }

    @Test
    public void downloadWritesToFileStorage() throws IOException, EncryptionException {
        when(httpClient.downloadFile(streamUrl)).thenReturn(response);

        operations.download(downloadRequest);

        InOrder inOrder = inOrder(fileStorage, response);
        inOrder.verify(fileStorage).storeTrack(trackUrn, downloadStream);
        inOrder.verify(response).close();
    }

    @Test
    public void returnsDownloadFailedWhenIOError() throws IOException {
        final IOException ioException = new IOException("Test IOException");
        when(httpClient.downloadFile(streamUrl)).thenThrow(ioException);

        expect(operations.download(downloadRequest).isSuccess()).toBeFalse();
    }

    @Test
    public void returnsDownloadFailedWhenEncryptionFailed() throws IOException, EncryptionException {
        final EncryptionException encryptionException = new EncryptionException("Test EncryptionException", null);
        when(httpClient.downloadFile(streamUrl)).thenReturn(response);
        doThrow(encryptionException).when(fileStorage).storeTrack(trackUrn, downloadStream);

        expect(operations.download(downloadRequest).isSuccess()).toBeFalse();
    }

    @Test
    public void returnsDownloadFailedWhenServerError() throws IOException, EncryptionException {
        when(httpClient.downloadFile(streamUrl)).thenReturn(response);
        when(response.isFailure()).thenReturn(true);
        when(response.isUnavailable()).thenReturn(false);

        expect(operations.download(downloadRequest).isFailure()).toBeTrue();
    }

    @Test
    public void returnsFileUnavailableWhenTrackUnavailable() throws IOException, EncryptionException {
        when(httpClient.downloadFile(streamUrl)).thenReturn(response);
        when(response.isFailure()).thenReturn(true);
        when(response.isUnavailable()).thenReturn(true);

        expect(operations.download(downloadRequest).isUnavailable()).toBeTrue();
    }

    @Test
    public void deletesFileFromFailedIO() throws IOException, EncryptionException {
        final IOException ioException = new IOException("Test IOException");
        doThrow(ioException).when(fileStorage).storeTrack(trackUrn, downloadStream);

        operations.download(downloadRequest);

        verify(fileStorage).deleteTrack(trackUrn);
    }

    @Test
    public void deletesFileFromFailedEncryption() throws IOException, EncryptionException {
        final EncryptionException encryptionException = new EncryptionException("Test EncryptionException", null);
        when(httpClient.downloadFile(streamUrl)).thenReturn(response);
        doThrow(encryptionException).when(fileStorage).storeTrack(trackUrn, downloadStream);

        operations.download(downloadRequest);

        verify(fileStorage).deleteTrack(trackUrn);
    }

    @Test
    public void doesNotStoreFileWhenResponseIsNotSuccess() {
        when(response.isFailure()).thenReturn(true);

        operations.download(downloadRequest);

        verifyZeroInteractions(fileStorage);
    }

    @Test
    public void invalidNetworkWhenDisconnected() {
        when(connectionHelper.isNetworkConnected()).thenReturn(false);

        expect(operations.isValidNetwork()).toBeFalse();
    }

    @Test
    public void validNetworkWhenConnectedAndAllNetworkAllowed() {
        when(offlineSettings.isWifiOnlyEnabled()).thenReturn(false);
        when(connectionHelper.isNetworkConnected()).thenReturn(true);

        expect(operations.isValidNetwork()).toBeTrue();
    }

    @Test
    public void invalidNetworkWhenNotConnectedOnWifiAndOnlyWifiAllowed() {
        when(offlineSettings.isWifiOnlyEnabled()).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(true);
        when(connectionHelper.isWifiConnected()).thenReturn(false);

        expect(operations.isValidNetwork()).toBeFalse();
    }

    @Test
    public void validNetworkWhenConnectedOnWifiAndOnlyWifiAllowed() {
        when(offlineSettings.isWifiOnlyEnabled()).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(true);
        when(connectionHelper.isWifiConnected()).thenReturn(true);

        expect(operations.isValidNetwork()).toBeTrue();
    }
}
