package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class DownloadQueueTest extends AndroidUnitTest {

    private static final Urn TRACK1 = Urn.forTrack(123);
    private static final Urn TRACK2 = Urn.forTrack(456L);
    private static final Urn PLAYLIST1 = Urn.forPlaylist(123L);
    private static final Urn PLAYLIST2 = Urn.forPlaylist(456L);
    private static final long TRACK_DURATION = 12345L;

    private DownloadQueue downloadQueue;

    @Before
    public void setUp() throws Exception {
        downloadQueue = new DownloadQueue();
    }

    @Test
    public void isEmptyReturnsTrue() {
        downloadQueue.set(Collections.<DownloadRequest>emptyList());

        assertThat(downloadQueue.isEmpty()).isTrue();
    }

    @Test
    public void isEmptyReturnsFalse() {
        downloadQueue.set(Arrays.asList(createDownloadRequest(TRACK1)));

        assertThat(downloadQueue.isEmpty()).isFalse();
    }

    @Test
    public void pollReturnsAndRemoveTheFirstRequest() {
        final DownloadRequest request1 = createDownloadRequest(TRACK1);
        final DownloadRequest request2 = createDownloadRequest(TRACK2);
        downloadQueue.set(Arrays.asList(request1, request2));

        assertThat(downloadQueue.poll()).isEqualTo(request1);
        assertThat(downloadQueue.getRequests()).containsExactly(request2);
    }

    @Test
    public void getRequestedReturnsAnEmptyListWhenPlaylistsFromTheResultAreNotInTheQueue() {
        final DownloadRequest request1 = createDownloadRequest(TRACK1, PLAYLIST1);
        final DownloadRequest request2 = createDownloadRequest(TRACK2, PLAYLIST2);
        downloadQueue.set(Arrays.asList(request1));

        assertThat(downloadQueue.getRequested(DownloadState.success(request2))).isEmpty();
    }

    @Test
    public void getRequestedReturnsPlaylistsPendingInTheQueue() {
        final DownloadRequest request1 = createDownloadRequest(TRACK1, PLAYLIST1);
        final DownloadRequest request2 = createDownloadRequest(TRACK2, PLAYLIST1);
        downloadQueue.set(Arrays.asList(request1));

        assertThat(downloadQueue.getRequested(DownloadState.success(request2))).containsExactly(PLAYLIST1);
    }

    @Test
    public void getRequestedReturnsPlaylistsWithoutDuplications() {
        final DownloadRequest request1 = createDownloadRequest(TRACK1, PLAYLIST1, PLAYLIST2);
        final DownloadRequest request2 = createDownloadRequest(TRACK2, PLAYLIST1, PLAYLIST2);
        downloadQueue.set(Arrays.asList(request1, request2));

        assertThat(downloadQueue.getRequested(DownloadState.success(request1)))
                .containsExactly(PLAYLIST1, PLAYLIST2);
    }

    @Test
    public void getDownloadedReturnsTheTrackAndThePlaylistsWhenNoPlaylistsPendingInTheQueue() {
        final DownloadRequest request1 = createDownloadRequest(TRACK1, PLAYLIST1);
        final DownloadRequest request2 = createDownloadRequest(TRACK2, PLAYLIST2);
        downloadQueue.set(Arrays.asList(request1));

        assertThat(downloadQueue.getDownloaded(DownloadState.success(request2))).contains(TRACK2, PLAYLIST2);
    }

    @Test
    public void getDownloadedReturnsTheTrackWhenPlaylistsPendingInTheQueue() {
        final DownloadRequest request1 = createDownloadRequest(TRACK1, PLAYLIST1);
        final DownloadRequest request2 = createDownloadRequest(TRACK2, PLAYLIST1);
        downloadQueue.set(Arrays.asList(request1));

        assertThat(downloadQueue.getDownloaded(DownloadState.success(request2))).contains(TRACK2);
    }

    @Test
    public void getDownloadedCollectionsReturnsOnlyPlaylistsWhenItsNotPendingAnymore() {
        final DownloadRequest request1 = createDownloadRequest(TRACK1, PLAYLIST1);
        final DownloadRequest request2 = createDownloadRequest(TRACK2, PLAYLIST2);

        downloadQueue.set(Arrays.asList(request2));

        assertThat(downloadQueue.getDownloadedPlaylists(DownloadState.canceled(request1))).contains(PLAYLIST1);
    }

    @Test
    public void getDownloadedCollectionsDoesNotReturnPlaylistWhenStillPending() {
        final DownloadRequest request1 = createDownloadRequest(TRACK1, PLAYLIST1);
        final DownloadRequest request2 = createDownloadRequest(TRACK2, PLAYLIST1);

        downloadQueue.set(Arrays.asList(request2));

        assertThat(downloadQueue.getDownloadedPlaylists(DownloadState.canceled(request1))).isEmpty();
    }

    @Test
    public void getUnavailableReturnsTheTrackAndThePlaylistsWhenNoPlaylistsPendingInTheQueue() {
        final DownloadRequest request1 = createDownloadRequest(TRACK1, PLAYLIST1);
        final DownloadRequest request2 = createDownloadRequest(TRACK2, PLAYLIST2);
        downloadQueue.set(Arrays.asList(request1));

        assertThat(downloadQueue.getDownloaded(DownloadState.success(request2))).contains(TRACK2, PLAYLIST2);
    }

    @Test
    public void getUnavailableReturnsTheTrackWhenPlaylistsPendingInTheQueue() {
        final DownloadRequest request1 = createDownloadRequest(TRACK1, PLAYLIST1);
        final DownloadRequest request2 = createDownloadRequest(TRACK2, PLAYLIST1);
        downloadQueue.set(Arrays.asList(request1));

        assertThat(downloadQueue.getDownloaded(DownloadState.success(request2))).contains(TRACK2);
    }

    @Test
    public void isAllLikedTracksDownloadedReturnsTrueWhenRequestDownloadedALikeAndNoneLikedTrackIsRequested() {
        final DownloadRequest request1 = createDownloadRequest(TRACK1, true);
        downloadQueue.set(Collections.<DownloadRequest>emptyList());

        assertThat(downloadQueue.isAllLikedTracksDownloaded(DownloadState.success(request1))).isTrue();
    }

    @Test
    public void isAllLikedTracksDownloadedReturnsFalseRequestIsNotRelatedToLikedTrack() {
        final DownloadRequest request1 = createDownloadRequest(TRACK1, false);
        downloadQueue.set(Collections.<DownloadRequest>emptyList());

        assertThat(downloadQueue.isAllLikedTracksDownloaded(DownloadState.success(request1))).isFalse();
    }

    @Test
    public void isAllLikedTracksDownloadedReturnsFalseWhenLikedTrackRequested() {
        final DownloadRequest request1 = createDownloadRequest(TRACK1, true);
        final DownloadRequest request2 = createDownloadRequest(TRACK2, true);
        downloadQueue.set(Arrays.asList(request2));

        assertThat(downloadQueue.isAllLikedTracksDownloaded(DownloadState.success(request1))).isFalse();
    }

    private DownloadRequest createDownloadRequest(Urn track, boolean isLikedTrack) {
        return new DownloadRequest.Builder(track, TRACK_DURATION, "http://wav")
                .addToLikes(isLikedTrack)
                .build();
    }

    private DownloadRequest createDownloadRequest(Urn track, Urn playlist) {
        return new DownloadRequest.Builder(track, TRACK_DURATION, "http://wav")
                .addToPlaylist(playlist)
                .build();
    }

    private DownloadRequest createDownloadRequest(Urn track, Urn playlist1, Urn playlist2) {
        return new DownloadRequest.Builder(track, TRACK_DURATION, "http://wav")
                .addToPlaylist(playlist1)
                .addToPlaylist(playlist2)
                .build();
    }

    private DownloadRequest createDownloadRequest(Urn track) {
        return new DownloadRequest.Builder(track, TRACK_DURATION, "http://wav").build();
    }

}