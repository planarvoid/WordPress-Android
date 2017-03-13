package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.propeller.PropellerWriteException;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TrackDownloadsStorageTest extends StorageIntegrationTest {

    private static final Urn TRACK_1 = Urn.forTrack(123L);
    private static final Urn TRACK_2 = Urn.forTrack(456L);
    private static final DownloadRequest request = ModelFixtures.downloadRequestFromLikes(TRACK_1);

    private TrackDownloadsStorage storage;
    private TestDateProvider dateProvider;
    private TestSubscriber<List<Urn>> listSubscriber;
    private TestSubscriber<OfflineState> offlineStateSubscriber;

    @Before
    public void setup() {
        dateProvider = new TestDateProvider();
        storage = new TrackDownloadsStorage(propeller(), propellerRx(), dateProvider);
        listSubscriber = new TestSubscriber<>();
        offlineStateSubscriber = new TestSubscriber<>();
    }

    @Test
    public void getLikesOfflineStateReturnsDownloadedWhenAllLikesAreDownloaded() {
        insertOfflineLikeDownloadCompleted(100);
        insertOfflineLikeDownloadCompleted(200);

        storage.getLikesOfflineState().subscribe(offlineStateSubscriber);

        offlineStateSubscriber.assertValue(OfflineState.DOWNLOADED);
    }

    @Test
    public void getLikedOfflineStateReturnsRequestedWhenSomeTracksWereNotYetDownloaded() {
        insertOfflineLikeDownloadCompleted(100);
        insertOfflineLikePendingDownload(200);

        storage.getLikesOfflineState().subscribe(offlineStateSubscriber);

        offlineStateSubscriber.assertValue(OfflineState.REQUESTED);
    }

    @Test
    public void getLikedOfflineStateReturnsUnavailableAllTracksWereCreatorOptOut() {
        insertOfflineLikeCreatorOptOut();
        insertOfflineLikeCreatorOptOut();

        storage.getLikesOfflineState().subscribe(offlineStateSubscriber);

        offlineStateSubscriber.assertValue(OfflineState.UNAVAILABLE);
    }

    @Test
    public void getLikedOfflineStateReturnsDownloadedEventWhenSomeTracksWereCreatorOptOut() {
        insertOfflineLikeDownloadCompleted(100);
        insertOfflineLikeCreatorOptOut();

        storage.getLikesOfflineState().subscribe(offlineStateSubscriber);

        offlineStateSubscriber.assertValue(OfflineState.DOWNLOADED);
    }

    @Test
    public void getTracksToRemoveReturnsTrackPendingRemovalSinceAtLeast3Minutes() {
        final long fourMinutesAgo = dateProvider.getCurrentTime() - TimeUnit.MINUTES.toMillis(4);

        testFixtures().insertTrackDownloadPendingRemoval(TRACK_1, dateProvider.getCurrentTime());
        testFixtures().insertTrackDownloadPendingRemoval(TRACK_2, fourMinutesAgo);

        storage.getTracksToRemove().subscribe(listSubscriber);

        listSubscriber.assertValue(Collections.singletonList(TRACK_2));
    }

    @Test
    public void updatesDownloadTracksWithDownloadResults() throws PropellerWriteException {
        final DownloadState downloadState = DownloadState.success(request);
        testFixtures().insertTrackPendingDownload(TRACK_1, 100L);

        storage.storeCompletedDownload(downloadState);

        databaseAssertions().assertDownloadResultsInserted(downloadState);
    }

    @Test
    public void resetUnavailableAtWhenDownloaded() {
        testFixtures().insertUnavailableTrackDownload(TRACK_1, 100L);

        final DownloadState downloadState = DownloadState.success(request);
        storage.storeCompletedDownload(downloadState);

        databaseAssertions().assertDownloadIsAvailable(TRACK_1);
    }

    @Test
    public void markTrackAsUnavailable() throws Exception {
        testFixtures().insertTrackPendingDownload(TRACK_1, 100L);

        storage.markTrackAsUnavailable(TRACK_1);

        databaseAssertions().assertTrackIsUnavailable(TRACK_1, dateProvider.getCurrentTime());
    }

    @Test
    public void returnsDownloadStatesForAllTracks() {
        testFixtures().insertTrackPendingDownload(Urn.forTrack(1), 100L);
        testFixtures().insertUnavailableTrackDownload(Urn.forTrack(2), new Date().getTime());
        testFixtures().insertTrackDownloadPendingRemoval(Urn.forTrack(3), new Date(200).getTime());
        testFixtures().insertCompletedTrackDownload(Urn.forTrack(4), 100, 200);

        final TestSubscriber<Map<Urn, OfflineState>> subscriber = new TestSubscriber<>();
        storage.getOfflineStates().subscribe(subscriber);

        final HashMap<Urn, OfflineState> expectedStates = new HashMap<>();
        expectedStates.put(Urn.forTrack(1), OfflineState.REQUESTED);
        expectedStates.put(Urn.forTrack(2), OfflineState.UNAVAILABLE);
        expectedStates.put(Urn.forTrack(3), OfflineState.NOT_OFFLINE);
        expectedStates.put(Urn.forTrack(4), OfflineState.DOWNLOADED);

        subscriber.assertReceivedOnNext(Collections.singletonList(expectedStates));
    }

    @Test
    public void returnsFirstTrackUrnAvailableOffline() {
        List<Urn> tracks = Arrays.asList(TRACK_1, TRACK_2);
        testFixtures().insertCompletedTrackDownload(TRACK_2, 100, 200);

        List<Urn> offlineTracks = storage.onlyOfflineTracks(tracks);
        assertThat(offlineTracks).containsExactly(TRACK_2);
    }

    @Test
    public void returnsUrnNotSetWhenNoTracksAreAvailableOffline() {
        List<Urn> tracks = Arrays.asList(TRACK_1, TRACK_2);

        List<Urn> offlineTracks = storage.onlyOfflineTracks(tracks);
        assertThat(offlineTracks).isEmpty();
    }

    @Test
    public void onlyOfflineTracksHandlesHugePlaylists() {
        final int bigPlaylist = 1002;
        final List<Urn> tracks = new ArrayList<>();
        for (int i = 0; i < bigPlaylist; i++) {
            final Urn track = Urn.forTrack(i);
            tracks.add(track);
            testFixtures().insertCompletedTrackDownload(track, 100, 200);
        }

        List<Urn> offlineTracks = storage.onlyOfflineTracks(tracks);
        assertThat(offlineTracks).isEqualTo(tracks);
    }

    private Urn insertOfflineLikeCreatorOptOut() {
        final ApiTrack track = testFixtures().insertLikedTrack(new Date(100));
        testFixtures().insertCompletedTrackDownload(track.getUrn(), 100, 200);
        testFixtures().insertUnavailableTrackDownload(track.getUrn(), 300);

        return track.getUrn();
    }

    private Urn insertOfflineLikeDownloadCompleted(long likedAt) {
        final ApiTrack track = testFixtures().insertLikedTrack(new Date(likedAt));
        testFixtures().insertCompletedTrackDownload(track.getUrn(), 100, 200);

        return track.getUrn();
    }

    private Urn insertOfflineLikePendingDownload(long likedAt) {
        final ApiTrack track = testFixtures().insertLikedTrack(new Date(likedAt));
        testFixtures().insertTrackPendingDownload(track.getUrn(), 100);

        return track.getUrn();
    }

}
