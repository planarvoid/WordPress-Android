package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;

@RunWith(SoundCloudTestRunner.class)
public class DownloadQueueTest {
    private static final Urn TRACK1 = Urn.forTrack(123);
    private static final Urn TRACK2 = Urn.forTrack(456L);
    private static final Urn PLAYLIST1 = Urn.forPlaylist(123L);
    private static final Urn PLAYLIST2 = Urn.forPlaylist(456L);

    private DownloadQueue downloadQueue;

    @Before
    public void setUp() throws Exception {
        downloadQueue = new DownloadQueue();
    }

    @Test
    public void isEmptyReturnsTrue() {
        downloadQueue.set(Collections.<DownloadRequest>emptyList());

        expect(downloadQueue.isEmpty()).toBeTrue();
    }

    @Test
    public void isEmptyReturnsFalse() {
        downloadQueue.set(Arrays.asList(createDownloadRequest(TRACK1)));

        expect(downloadQueue.isEmpty()).toBeFalse();
    }

    @Test
    public void pollReturnsAndRemoveTheFirstRequest() {
        final DownloadRequest request1 = createDownloadRequest(TRACK1);
        final DownloadRequest request2 = createDownloadRequest(TRACK2);
        downloadQueue.set(Arrays.asList(request1, request2));

        expect(downloadQueue.poll()).toBe(request1);
        expect(downloadQueue.getRequests()).toContainExactly(request2);
    }

    @Test
    public void getRequestedReturnsAnEmptyListWhenPlaylistsFrmTheResultAreNotInTheQueue() {
        final DownloadRequest request1 = createDownloadRequest(TRACK1, PLAYLIST1);
        final DownloadRequest request2 = createDownloadRequest(TRACK2, PLAYLIST2);
        downloadQueue.set(Arrays.asList(request1));

        expect(downloadQueue.getRequested(DownloadResult.success(request2))).toBeEmpty();
    }

    @Test
    public void getRequestedReturnsPlaylistsPendingInTheQueue() {
        final DownloadRequest request1 = createDownloadRequest(TRACK1, PLAYLIST1);
        final DownloadRequest request2 = createDownloadRequest(TRACK2, PLAYLIST1);
        downloadQueue.set(Arrays.asList(request1));

        expect(downloadQueue.getRequested(DownloadResult.success(request2))).toContainExactly(PLAYLIST1);
    }

    @Test
    public void getCompletedReturnsTheTrackAndThePlaylistsWhenNoPlaylistsPendingInTheQueue() {
        final DownloadRequest request1 = createDownloadRequest(TRACK1, PLAYLIST1);
        final DownloadRequest request2 = createDownloadRequest(TRACK2, PLAYLIST2);
        downloadQueue.set(Arrays.asList(request1));

        expect(downloadQueue.getDownloaded(DownloadResult.success(request2))).toContainExactlyInAnyOrder(TRACK2, PLAYLIST2);
    }

    @Test
    public void getCompletedReturnsTheTrackWhenPlaylistsPendingInTheQueue() {
        final DownloadRequest request1 = createDownloadRequest(TRACK1, PLAYLIST1);
        final DownloadRequest request2 = createDownloadRequest(TRACK2, PLAYLIST1);
        downloadQueue.set(Arrays.asList(request1));

        expect(downloadQueue.getDownloaded(DownloadResult.success(request2))).toContainExactlyInAnyOrder(TRACK2);
    }

    @Test
    public void getUnavailableReturnsTheTrackAndThePlaylistsWhenNoPlaylistsPendingInTheQueue() {
        final DownloadRequest request1 = createDownloadRequest(TRACK1, PLAYLIST1);
        final DownloadRequest request2 = createDownloadRequest(TRACK2, PLAYLIST2);
        downloadQueue.set(Arrays.asList(request1));

        expect(downloadQueue.getDownloaded(DownloadResult.success(request2))).toContainExactlyInAnyOrder(TRACK2, PLAYLIST2);
    }

    @Test
    public void getUnavailableReturnsTheTrackWhenPlaylistsPendingInTheQueue() {
        final DownloadRequest request1 = createDownloadRequest(TRACK1, PLAYLIST1);
        final DownloadRequest request2 = createDownloadRequest(TRACK2, PLAYLIST1);
        downloadQueue.set(Arrays.asList(request1));

        expect(downloadQueue.getDownloaded(DownloadResult.success(request2))).toContainExactlyInAnyOrder(TRACK2);
    }

    private DownloadRequest createDownloadRequest(Urn track1, Urn... playlists) {
        final DownloadRequest.Builder builder = new DownloadRequest.Builder(track1, "http://");
        for (Urn playlist : playlists) {
            builder.addToPlaylist(playlist);
        }
        return builder.build();
    }

    private DownloadRequest createDownloadRequest(Urn track1) {
        return new DownloadRequest.Builder(track1, "http://").build();
    }

}