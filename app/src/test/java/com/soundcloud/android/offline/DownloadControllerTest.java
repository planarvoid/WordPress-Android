package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class DownloadControllerTest {

    @Mock private StrictSSLHttpClient httpClient;
    @Mock private SecureFileStorage trackStorage;
    @Mock private TrackDownloadsStorage downloadsStorage;
    @Mock private InputStream inputStream;

    private DownloadController controller;

    private final Urn TRACK1_URN = Urn.forTrack(123L);
    private final Urn TRACK2_URN = Urn.forTrack(124L);
    private final String TRACK1_STREAM_URL = "http://stream1.url";
    private final String TRACK2_STREAM_URL = "http://stream2.url";

    @Before
    public void setUp() throws Exception {
        controller = new DownloadController(httpClient, trackStorage, downloadsStorage);
        when(downloadsStorage.getPendingDownloads()).thenReturn(listOf1PendingDownload());
    }

    @Test
    public void getsTracksToDownloadFromDownloadStorage() throws IOException {
        controller.downloadTracks();

        verify(downloadsStorage).getPendingDownloads();
        verify(httpClient).downloadFile(TRACK1_STREAM_URL);
    }

    @Test
    public void updatesDownloadStateInStorageAfterSuccessfulDownload() throws Exception {
        when(httpClient.downloadFile(TRACK1_STREAM_URL)).thenReturn(inputStream);

        controller.downloadTracks();

        ArgumentCaptor<DownloadResult> captor = ArgumentCaptor.forClass(DownloadResult.class);
        InOrder inOrder = inOrder(trackStorage, inputStream, downloadsStorage);

        inOrder.verify(trackStorage).storeTrack(TRACK1_URN, inputStream);
        inOrder.verify(inputStream).close();
        inOrder.verify(downloadsStorage).updateDownload(captor.capture());

        expect(captor.getValue().getUrn()).toEqual(TRACK1_URN);
        expect(captor.getValue().isSuccessful()).toBeTrue();
    }

    @Test
    public void doesNotUpdateDownloadStateWhenDownloadFailed() throws Exception {
        when(httpClient.downloadFile(TRACK1_STREAM_URL)).thenThrow(new IOException("Test IOException"));

        controller.downloadTracks();
        verify(downloadsStorage, never()).updateDownload(any(DownloadResult.class));
    }

    @Test
    public void doesNotUpdateDownloadStateWhenEncryptionFailed() throws Exception {
        when(httpClient.downloadFile(TRACK1_STREAM_URL)).thenReturn(inputStream);

        doThrow(new EncryptionException("Test EncryptionExceotion", null))
                .when(trackStorage).storeTrack(TRACK1_URN, inputStream);

        controller.downloadTracks();
        verify(downloadsStorage, never()).updateDownload(any(DownloadResult.class));
    }

    @Test
    public void doNotDownloadTracksWhenNothingToDownload() throws IOException {
        when(downloadsStorage.getPendingDownloads()).thenReturn(new LinkedList<DownloadRequest>());

        controller.downloadTracks();

        verify(httpClient, never()).downloadFile(anyString());
    }

    @Test
    public void downloadTracksWhenPresent() throws IOException {
        when(downloadsStorage.getPendingDownloads()).thenReturn(listOf2PendingDownloads());

        controller.downloadTracks();

        InOrder inOrder = inOrder(httpClient);
        inOrder.verify(httpClient).downloadFile(TRACK1_STREAM_URL);
        inOrder.verify(httpClient).downloadFile(TRACK2_STREAM_URL);
    }

    private List<DownloadRequest> listOf2PendingDownloads() {
        return Arrays.asList(
                new DownloadRequest(TRACK1_URN, TRACK1_STREAM_URL),
                new DownloadRequest(TRACK2_URN, TRACK2_STREAM_URL));
    }

    private List<DownloadRequest> listOf1PendingDownload() {
        return Arrays.asList(new DownloadRequest(TRACK1_URN, TRACK1_STREAM_URL));
    }
}