package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.crypto.EncryptionInterruptedException;
import com.soundcloud.android.crypto.Encryptor;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.StreamUrlBuilder;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.io.InputStream;

public class DownloadOperationsTest extends AndroidUnitTest {

    @Mock private StrictSSLHttpClient httpClient;
    @Mock private SecureFileStorage fileStorage;
    @Mock private StrictSSLHttpClient.TrackFileResponse response;
    @Mock private DeleteOfflineTrackCommand deleteOfflineContent;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private InputStream downloadStream;
    @Mock private NetworkConnectionHelper connectionHelper;
    @Mock private OfflineSettingsStorage offlineSettings;
    @Mock private StreamUrlBuilder streamUrlBuilder;
    @Mock private DownloadOperations.DownloadProgressListener listener;
    @Mock private OfflineTrackAssetDownloader assetDownloader;

    private DownloadOperations operations;

    private final Urn trackUrn = Urn.forTrack(123L);
    private final String streamUrl = "http://stream1.url";
    private final long trackDuration = 12345;
    private final DownloadRequest downloadRequest = ModelFixtures.downloadRequestFromLikes(trackUrn);

    @Before
    public void setUp() throws Exception {
        operations = new DownloadOperations(httpClient, fileStorage, deleteOfflineContent, playQueueManager,
                connectionHelper, offlineSettings, streamUrlBuilder, Schedulers.immediate(), assetDownloader);
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

        operations.download(downloadRequest, listener);

        InOrder inOrder = inOrder(streamUrlBuilder, fileStorage, assetDownloader, response);
        inOrder.verify(streamUrlBuilder).buildHttpsStreamUrl(downloadRequest.getTrack());
        inOrder.verify(fileStorage).storeTrack(eq(trackUrn), same(downloadStream), any(Encryptor.EncryptionProgressListener.class));
        inOrder.verify(assetDownloader).fetchTrackArtwork(downloadRequest.getTrack());
        inOrder.verify(assetDownloader).fetchTrackWaveform(downloadRequest.getTrack(), downloadRequest.getWaveformUrl());
        inOrder.verify(response).close();
    }

    @Test
    public void reportsProgressToListener() throws IOException, EncryptionException {
        when(httpClient.getFileStream(streamUrl)).thenReturn(response);

        operations.download(downloadRequest, listener);

        ArgumentCaptor<Encryptor.EncryptionProgressListener> listenerArgumentCaptor = ArgumentCaptor.forClass(Encryptor.EncryptionProgressListener.class);
        verify(fileStorage).storeTrack(eq(trackUrn), same(downloadStream), listenerArgumentCaptor.capture());
        listenerArgumentCaptor.getValue().onBytesEncrypted(1000L);

        verify(listener).onProgress(1000L);
    }

    @Test
    public void returnsDownloadFailedWhenIOError() throws IOException {
        final IOException ioException = new IOException("Test IOException");
        when(httpClient.getFileStream(streamUrl)).thenThrow(ioException);

        assertThat(operations.download(downloadRequest, listener).isSuccess()).isFalse();
    }

    @Test
    public void returnsDownloadFailedWhenEncryptionFailed() throws IOException, EncryptionException {
        final EncryptionException encryptionException = new EncryptionException("Test EncryptionException", null);
        when(httpClient.getFileStream(streamUrl)).thenReturn(response);
        doThrow(encryptionException).when(fileStorage).storeTrack(eq(trackUrn), same(downloadStream),
                any(Encryptor.EncryptionProgressListener.class));

        assertThat(operations.download(downloadRequest, listener).isSuccess()).isFalse();
    }

    @Test
    public void returnsDownloadFailedWhenServerError() throws IOException {
        when(httpClient.getFileStream(streamUrl)).thenReturn(response);
        when(response.isSuccess()).thenReturn(false);
        when(response.isFailure()).thenReturn(true);
        when(response.isUnavailable()).thenReturn(false);

        assertThat(operations.download(downloadRequest, listener).isDownloadFailed()).isTrue();
    }

    @Test
    public void returnConnectionErrorWhenIOExceptionThrown() throws IOException {
        when(httpClient.getFileStream(streamUrl)).thenThrow(new IOException());

        assertThat(operations.download(downloadRequest, listener).isConnectionError()).isTrue();
    }

    @Test
    public void returnsFileUnavailableWhenTrackUnavailable() throws IOException {
        when(httpClient.getFileStream(streamUrl)).thenReturn(response);
        when(response.isSuccess()).thenReturn(false);
        when(response.isFailure()).thenReturn(true);
        when(response.isUnavailable()).thenReturn(true);

        assertThat(operations.download(downloadRequest, listener).isUnavailable()).isTrue();
    }

    @Test
    public void cancelCallsTryCancelRunningDownloadAndEncrypt() throws IOException {
        when(httpClient.getFileStream(streamUrl)).thenReturn(response);

        operations.cancelCurrentDownload();

        verify(fileStorage).tryCancelRunningEncryption();
    }

    @Test
    public void canceledDownloadReturnsDownloadCancelledResult() throws IOException, EncryptionException {
        doThrow(new EncryptionInterruptedException("boom!")).when(fileStorage)
                .storeTrack(eq(trackUrn), same(downloadStream), any(Encryptor.EncryptionProgressListener.class));

        DownloadState result = operations.download(downloadRequest, listener);

        assertThat(result.isCancelled()).isTrue();
    }

    @Test
    public void doesNotPrefetchArtworkWhenDownloadFailedOrWasCanceled() throws IOException, EncryptionException {
        doThrow(new EncryptionInterruptedException("boom!")).when(fileStorage)
                .storeTrack(eq(trackUrn), same(downloadStream), any(Encryptor.EncryptionProgressListener.class));

        operations.download(downloadRequest, listener);

        verifyZeroInteractions(assetDownloader);
    }

    @Test
    public void doesNotDownloadTrackWhenNotEnoughSpace() {
        when(fileStorage.isEnoughSpaceForTrack(anyLong())).thenReturn(false);

        operations.download(downloadRequest, listener);

        verifyZeroInteractions(httpClient);
    }

    @Test
    public void doesNotStoreTrackWhenNotEnoughSpace() throws IOException, EncryptionException {
        when(fileStorage.isEnoughSpaceForTrack(anyLong())).thenReturn(false);

        operations.download(downloadRequest, listener);

        verify(fileStorage, never()).storeTrack(eq(trackUrn), same(downloadStream), any(Encryptor.EncryptionProgressListener.class));
    }

    @Test
    public void returnsNotEnoughSpaceResult() {
        when(fileStorage.isEnoughSpaceForTrack(anyLong())).thenReturn(false);

        assertThat(operations.download(downloadRequest, listener).isNotEnoughSpace()).isTrue();
    }

    @Test
    public void doesNotStoreFileWhenResponseIsNotSuccess() throws IOException, EncryptionException {
        when(response.isSuccess()).thenReturn(false);
        when(response.isFailure()).thenReturn(true);

        operations.download(downloadRequest, listener);

        verify(fileStorage, never()).storeTrack(eq(trackUrn), same(downloadStream), any(Encryptor.EncryptionProgressListener.class));
    }

    @Test
    public void invalidNetworkWhenDisconnected() {
        when(connectionHelper.isNetworkConnected()).thenReturn(false);

        assertThat(operations.isValidNetwork()).isFalse();
    }

    @Test
    public void validNetworkWhenConnectedAndAllNetworkAllowed() {
        when(offlineSettings.isWifiOnlyEnabled()).thenReturn(false);
        when(connectionHelper.isNetworkConnected()).thenReturn(true);

        assertThat(operations.isValidNetwork()).isTrue();
    }

    @Test
    public void invalidNetworkWhenNotConnectedOnWifiAndOnlyWifiAllowed() {
        when(offlineSettings.isWifiOnlyEnabled()).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(true);
        when(connectionHelper.isWifiConnected()).thenReturn(false);

        assertThat(operations.isValidNetwork()).isFalse();
    }

    @Test
    public void validNetworkWhenConnectedOnWifiAndOnlyWifiAllowed() {
        when(offlineSettings.isWifiOnlyEnabled()).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(true);
        when(connectionHelper.isWifiConnected()).thenReturn(true);

        assertThat(operations.isValidNetwork()).isTrue();
    }
}
