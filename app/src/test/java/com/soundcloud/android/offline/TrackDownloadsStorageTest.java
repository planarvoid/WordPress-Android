package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.propeller.PropellerWriteException;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestObserver;
import rx.observers.TestSubscriber;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TrackDownloadsStorageTest extends StorageIntegrationTest {

    private static final Urn TRACK_1 = Urn.forTrack(123L);
    private static final Urn TRACK_2 = Urn.forTrack(456L);
    private static final DownloadRequest request = new DownloadRequest(TRACK_1, 12345L, "http://wav");

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
    public void loadsOfflineLikesOrderedByLikeDate() {
        final Urn track1 = insertOfflineLikeDownloadCompleted(100);
        final Urn track2 = insertOfflineLikeDownloadCompleted(200);

        storage.likesUrns().subscribe(listSubscriber);

        listSubscriber.assertValue(Arrays.asList(track2, track1));
    }

    @Test
    public void doesNotLoadOfflineLikesPendingRemoval() {
        testFixtures().insertTrackDownloadPendingRemoval(TRACK_1, new Date(200).getTime());

        storage.likesUrns().subscribe(listSubscriber);

        listSubscriber.assertValue(Lists.<Urn>emptyList());
    }

    @Test
    public void doesNotLoadNonOfflineLike() {
        testFixtures().insertLikedTrack(new Date(100));

        storage.likesUrns().subscribe(listSubscriber);

        listSubscriber.assertValue(Lists.<Urn>emptyList());
    }

    @Test
    public void doesNotLoadCreatorOptOutLikeThatWasPreviouslyDownloaded() {
        insertOfflineLikeCreatorOptOut(100);

        storage.likesUrns().subscribe(listSubscriber);

        listSubscriber.assertValue(Lists.<Urn>emptyList());
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
        insertOfflineLikeCreatorOptOut(200);
        insertOfflineLikeCreatorOptOut(300);

        storage.getLikesOfflineState().subscribe(offlineStateSubscriber);

        offlineStateSubscriber.assertValue(OfflineState.UNAVAILABLE);
    }

    @Test
    public void getLikedOfflineStateReturnsDownloadedEventWhenSomeTracksWereCreatorOptOut() {
        insertOfflineLikeDownloadCompleted(100);
        insertOfflineLikeCreatorOptOut(300);

        storage.getLikesOfflineState().subscribe(offlineStateSubscriber);

        offlineStateSubscriber.assertValue(OfflineState.DOWNLOADED);
    }

    @Test
    public void loadsTracksFromAPlaylist() {
        final Urn playlistUrn = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();
        final Urn trackUrn1 = insertOfflinePlaylistTrack(playlistUrn, 0);
        final Urn trackUrn2 = insertOfflinePlaylistTrack(playlistUrn, 1);

        storage.playlistTrackUrns(playlistUrn).subscribe(listSubscriber);

        listSubscriber.assertValue(Arrays.asList(trackUrn1, trackUrn2));
    }

    @Test
    public void doesNotLoadTracksFromAPlaylistMarkedForRemoval() {
        final Urn playlistUrn = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();
        final ApiTrack track = testFixtures().insertPlaylistTrack(playlistUrn, 0);
        testFixtures().insertTrackDownloadPendingRemoval(track.getUrn(), new Date(200).getTime());

        storage.playlistTrackUrns(playlistUrn).subscribe(listSubscriber);

        listSubscriber.assertValue(Lists.<Urn>emptyList());
    }

    @Test
    public void doesNotLoadCreatorOptOutTracksFromAPlaylistThatWerePreviouslyDownloaded() {
        final Urn playlistUrn = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();
        final ApiTrack track = testFixtures().insertPlaylistTrack(playlistUrn, 0);
        testFixtures().insertCompletedTrackDownload(track.getUrn(), 100, 200);
        testFixtures().insertUnavailableTrackDownload(track.getUrn(), 300);

        storage.playlistTrackUrns(playlistUrn).subscribe(listSubscriber);

        listSubscriber.assertValue(Lists.<Urn>emptyList());
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

    private void insertCompletedDownload(long policyUpdate) {
        final ApiTrack track = insertTrackWithPolicy(policyUpdate);
        testFixtures().insertCompletedTrackDownload(track.getUrn(), System.currentTimeMillis(), System.currentTimeMillis());
    }

    private ApiTrack insertTrackWithPolicy(long policyUpdate) {
        final ApiTrack track = testFixtures().insertTrack();
        testFixtures().insertPolicyAllow(track.getUrn(), policyUpdate);
        return track;
    }

    private Urn insertOfflinePlaylistTrack(Urn playlist, int position) {
        final ApiTrack track = testFixtures().insertPlaylistTrack(playlist, position);
        testFixtures().insertCompletedTrackDownload(track.getUrn(), 0, 100);
        return track.getUrn();
    }

    private Urn insertOfflinePlaylistTrackPendingDownload(Urn playlist, int position) {
        final ApiTrack track = testFixtures().insertPlaylistTrack(playlist, position);
        testFixtures().insertTrackPendingDownload(track.getUrn(), 100);
        return track.getUrn();
    }

    private Urn insertOfflineLikeCreatorOptOut(long likedAt) {
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
