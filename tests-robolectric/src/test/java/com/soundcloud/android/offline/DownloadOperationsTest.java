package com.soundcloud.android.offline;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.crypto.EncryptionInterruptedException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.DeleteOfflineTrackCommand;
import com.soundcloud.android.playback.StreamUrlBuilder;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.io.InputStream;

@RunWith(SoundCloudTestRunner.class)
public class DownloadOperationsTest {

    @Mock private StrictSSLHttpClient httpClient;
    @Mock private SecureFileStorage fileStorage;
    @Mock private StrictSSLHttpClient.TrackFileResponse response;
    @Mock private DeleteOfflineTrackCommand deleteOfflineContent;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private InputStream downloadStream;
    @Mock private NetworkConnectionHelper connectionHelper;
    @Mock private OfflineSettingsStorage offlineSettings;
    @Mock private StreamUrlBuilder streamUrlBuilder;

    private DownloadOperations operations;

    private final Urn trackUrn = Urn.forTrack(123L);
    private final String streamUrl = "http://stream1.url";
    private final long trackDuration = 12345;
    private final DownloadRequest downloadRequest = new DownloadRequest(trackUrn, trackDuration);

    @Before
    public void setUp() throws Exception {
        operations = new DownloadOperations(httpClient, fileStorage, deleteOfflineContent, playQueueManager,
                connectionHelper, offlineSettings, streamUrlBuilder, Schedulers.immediate());
        when(streamUrlBuilder.buildHttpsStreamUrl(trackUrn)).thenReturn(streamUrl);
        when(httpClient.getFileStream(streamUrl)).thenReturn(response);
        when(response.isFailure()).thenReturn(false);
        when(response.isUnavailable()).thenReturn(false);
        when(response.isSuccess()).thenReturn(true);
        when(response.getInputStream()).thenReturn(downloadStream);
        when(fileStorage.isEnoughSpaceForTrack(anyLong())).thenReturn(true);
        when(connectionHelper.isWifiConnected()).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(true);
    }

    @Test
    public void downloadWritesToFileStorage() throws IOException, EncryptionException {
        when(httpClient.getFileStream(streamUrl)).thenReturn(response);

        operations.download(downloadRequest);

        InOrder inOrder = inOrder(streamUrlBuilder, fileStorage, response);
        inOrder.verify(streamUrlBuilder).buildHttpsStreamUrl(downloadRequest.track);
        inOrder.verify(fileStorage).storeTrack(trackUrn, downloadStream);
        inOrder.verify(response).close();
    }

    @Test
    public void returnsDownloadFailedWhenIOError() throws IOException {
        final IOException ioException = new IOException("Test IOException");
        when(httpClient.getFileStream(streamUrl)).thenThrow(ioException);

        expect(operations.download(downloadRequest).isSuccess()).toBeFalse();
    }

    @Test
    public void returnsDownloadFailedWhenEncryptionFailed() throws IOException, EncryptionException {
        final EncryptionException encryptionException = new EncryptionException("Test EncryptionException", null);
        when(httpClient.getFileStream(streamUrl)).thenReturn(response);
        doThrow(encryptionException).when(fileStorage).storeTrack(trackUrn, downloadStream);

        expect(operations.download(downloadRequest).isSuccess()).toBeFalse();
    }

    @Test
    public void returnsDownloadFailedWhenServerError() throws IOException {
        when(httpClient.getFileStream(streamUrl)).thenReturn(response);
        when(response.isSuccess()).thenReturn(false);
        when(response.isFailure()).thenReturn(true);
        when(response.isUnavailable()).thenReturn(false);

        expect(operations.download(downloadRequest).isDownloadFailed()).toBeTrue();
    }

    @Test
    public void returnConnectionErrorWhenIOExceptionThrown() throws IOException {
        when(httpClient.getFileStream(streamUrl)).thenThrow(new IOException());

        expect(operations.download(downloadRequest).isConnectionError()).toBeTrue();
    }

    @Test
    public void returnsFileUnavailableWhenTrackUnavailable() throws IOException {
        when(httpClient.getFileStream(streamUrl)).thenReturn(response);
        when(response.isSuccess()).thenReturn(false);
        when(response.isFailure()).thenReturn(true);
        when(response.isUnavailable()).thenReturn(true);

        expect(operations.download(downloadRequest).isUnavailable()).toBeTrue();
    }

    @Test
    public void cancelCallsTryCancelRunningDownloadAndEncrypt() throws IOException {
        when(httpClient.getFileStream(streamUrl)).thenReturn(response);

        operations.cancelCurrentDownload();

        verify(fileStorage).tryCancelRunningEncryption();
    }

    @Test
    public void cancelledDownloadReturnsDownloadCancelledResult() throws IOException, EncryptionException {
        doThrow(new EncryptionInterruptedException("boom!")).when(fileStorage).storeTrack(trackUrn, downloadStream);

        DownloadResult result = operations.download(downloadRequest);

        expect(result.isCancelled()).toBeTrue();
    }

    @Test
    public void doesNotDownloadTrackWhenNotEnoughSpace() {
        when(fileStorage.isEnoughSpaceForTrack(anyLong())).thenReturn(false);

        operations.download(downloadRequest);

        verifyZeroInteractions(httpClient);
    }

    @Test
    public void doesNotStoreTrackWhenNotEnoughSpace() throws IOException, EncryptionException {
        when(fileStorage.isEnoughSpaceForTrack(anyLong())).thenReturn(false);

        operations.download(downloadRequest);

        verify(fileStorage, never()).storeTrack(trackUrn, downloadStream);
    }

    @Test
    public void returnsNotEnoughSpaceResult() {
        when(fileStorage.isEnoughSpaceForTrack(anyLong())).thenReturn(false);

        expect(operations.download(downloadRequest).isNotEnoughSpace()).toBeTrue();
    }

    @Test
    public void doesNotStoreFileWhenResponseIsNotSuccess() throws IOException, EncryptionException {
        when(response.isSuccess()).thenReturn(false);
        when(response.isFailure()).thenReturn(true);

        operations.download(downloadRequest);

        verify(fileStorage, never()).storeTrack(trackUrn, downloadStream);
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
