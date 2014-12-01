package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class DownloadControllerTest {

    @Mock private DownloadHttpClient httpClient;
    @Mock private SecureFileStorage trackStorage;
    @Mock private TrackDownloadsStorage downloadsStorage;

    private DownloadController controller;

    private final Urn track1 = Urn.forTrack(123L);
    private final Urn track2 = Urn.forTrack(124L);
    private final String track1StreamUrl = "http://stream1.url";
    private final String track2StreamUrl = "http://stream2.url";

    @Before
    public void setUp() throws Exception {
        controller = new DownloadController(httpClient, trackStorage, downloadsStorage);
    }

    @Test
    public void getsTracksToDownloadFromDownloadStorage() throws IOException {
        List<DownloadRequest> requests = Arrays.asList(new DownloadRequest(track1, track1StreamUrl));
        when(downloadsStorage.getPendingDownloads()).thenReturn(requests);

        controller.downloadTracks();

        verify(downloadsStorage).getPendingDownloads();
        verify(httpClient).downloadFile(track1StreamUrl);
    }

    @Test
    public void updatesDownloadStateInStorageAfterSuccessfulDownload() throws IOException {
        when(downloadsStorage.getPendingDownloads()).thenReturn(Arrays.asList(new DownloadRequest(track1, track1StreamUrl)));
        when(httpClient.downloadFile(track1StreamUrl)).thenReturn(createInputStreamFor(track1StreamUrl));

        controller.downloadTracks();

        ArgumentCaptor<DownloadResult> captor = ArgumentCaptor.forClass(DownloadResult.class);
        InOrder inOrder = inOrder(trackStorage, downloadsStorage);

        inOrder.verify(trackStorage).storeTrack(eq(track1), any(InputStream.class));
        inOrder.verify(downloadsStorage).updateDownload(captor.capture());
        expect(captor.getValue().getUrn()).toEqual(track1);
    }

    @Test
    public void doNotDownloadTracksWhenNothingToDownload() throws IOException {
        when(downloadsStorage.getPendingDownloads()).thenReturn(new LinkedList<DownloadRequest>());

        controller.downloadTracks();

        verify(httpClient, never()).downloadFile(anyString());
    }

    @Test
    public void downloadTracksWhenPresent() throws IOException {
        final LinkedList<DownloadRequest> tracks = tracksMetadataForOfflineSync();
        when(downloadsStorage.getPendingDownloads()).thenReturn(tracks);

        controller.downloadTracks();

        InOrder inOrder = inOrder(httpClient);
        inOrder.verify(httpClient).downloadFile(track1StreamUrl);
        inOrder.verify(httpClient).downloadFile(track2StreamUrl);
    }

    private InputStream createInputStreamFor(String someMockContent){
        return new ByteArrayInputStream(someMockContent.getBytes(Charsets.UTF_8));
    }

    public LinkedList<DownloadRequest> tracksMetadataForOfflineSync() {
        LinkedList<DownloadRequest> sets = new LinkedList<>();
        sets.add(new DownloadRequest(track1, track1StreamUrl));
        sets.add(new DownloadRequest(track2, track2StreamUrl));
        return sets;
    }
}