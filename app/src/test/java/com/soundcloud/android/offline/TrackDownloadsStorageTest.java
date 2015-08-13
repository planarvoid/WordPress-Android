package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.PropellerWriteException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.TestObserver;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TrackDownloadsStorageTest extends StorageIntegrationTest {

    private static final Urn TRACK_1 = Urn.forTrack(123L);
    private static final Urn TRACK_2 = Urn.forTrack(456L);
    private static final DownloadRequest request = new DownloadRequest(TRACK_1, 12345L, "http://wav");
    @Mock private DateProvider dateProvider;

    private TrackDownloadsStorage storage;
    private TestObserver<List<Urn>> observer;

    @Before
    public void setup() {
        storage = new TrackDownloadsStorage(propeller(), propellerRx(), dateProvider);
        observer = new TestObserver<>();
    }

    @Test
    public void loadsOfflineLikesOrderedByLikeDate() {
        final Urn track1 = insertOfflineLikeDownloadCompleted(100);
        final Urn track2 = insertOfflineLikeDownloadCompleted(200);

        storage.likesUrns().subscribe(observer);

        assertThat(observer.getOnNextEvents().get(0)).containsExactly(track2, track1);
    }

    @Test
    public void doesNotLoadOfflineLikesPendingRemoval() {
        testFixtures().insertTrackDownloadPendingRemoval(TRACK_1, new Date(200).getTime());

        storage.likesUrns().subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0)).isEmpty();
    }

    @Test
    public void doesNotLoadNonOfflineLike() {
        testFixtures().insertLikedTrack(new Date(100));

        storage.likesUrns().subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0)).isEmpty();
    }

    @Test
    public void loadsOfflineLikedTracksPendingDownload() {
        insertOfflineLikeDownloadCompleted(100);
        final Urn track1 = insertOfflineLikePendingDownload(200);
        final Urn track2 = insertOfflineLikePendingDownload(300);

        storage.pendingLikedTracksUrns().subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0)).containsExactly(track1, track2);
    }

    @Test
    public void loadsOfflineTracksFromAPlaylist() {
        final Urn playlistUrn = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();
        final Urn trackUrn1 = insertOfflinePlaylistTrack(playlistUrn, 0);
        final Urn trackUrn2 = insertOfflinePlaylistTrack(playlistUrn, 1);

        storage.playlistTrackUrns(playlistUrn).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0)).containsExactly(trackUrn1, trackUrn2);
    }

    @Test
    public void doesNotLoadOfflineTracksFromAPlaylistMarkedForRemoval() {
        final Urn playlistUrn = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();
        final ApiTrack track = testFixtures().insertPlaylistTrack(playlistUrn, 0);
        testFixtures().insertTrackDownloadPendingRemoval(track.getUrn(), new Date(200).getTime());

        storage.playlistTrackUrns(playlistUrn).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0)).isEmpty();
    }

    @Test
    public void loadsOfflinePendingTrackFromAPlaylist() {
        final Urn playlistUrn = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();
        insertOfflinePlaylistTrack(playlistUrn, 0);
        final Urn trackUrn1 = insertOfflinePlaylistTrackPendingDownload(playlistUrn, 1);
        final Urn trackUrn2 = insertOfflinePlaylistTrackPendingDownload(playlistUrn, 2);

        storage.pendingPlaylistTracksUrns(playlistUrn).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0)).containsExactly(trackUrn1, trackUrn2);
    }

    @Test
    public void getTracksToRemoveReturnsTrackPendingRemovalSinceAtLeast3Minutes() {
        final Date now = new Date();
        final Date fourMinutesAgo = new Date(now.getTime() - TimeUnit.MINUTES.toMillis(4));
        when(dateProvider.getCurrentDate()).thenReturn(now);

        testFixtures().insertTrackDownloadPendingRemoval(TRACK_1, now.getTime());
        testFixtures().insertTrackDownloadPendingRemoval(TRACK_2, fourMinutesAgo.getTime());

        storage.getTracksToRemove().subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0)).containsExactly(TRACK_2);
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
        final Date now = new Date();
        when(dateProvider.getCurrentTime()).thenReturn(now.getTime());
        testFixtures().insertTrackPendingDownload(TRACK_1, 100L);

        storage.markTrackAsUnavailable(TRACK_1);

        databaseAssertions().assertTrackIsUnavailable(TRACK_1, now.getTime());
    }

    @Test
    public void getLastPolicyUpdateDoesNotEmitWhenNoPolicy() {
        final TestObserver<Long> observer = new TestObserver<>();

        storage.getLastPolicyUpdate().subscribe(observer);

        assertThat(observer.getOnNextEvents()).isEmpty();
        assertThat(observer.getOnCompletedEvents()).hasSize(1);
    }

    @Test
    public void getLastPolicyUpdateDoesNotEmitWhenNoTrackDownloadEntry() {
        final TestObserver<Long> observer = new TestObserver<>();
        insertTrackWithPolicy(System.currentTimeMillis());

        storage.getLastPolicyUpdate().subscribe(observer);

        assertThat(observer.getOnNextEvents()).isEmpty();
        assertThat(observer.getOnCompletedEvents()).hasSize(1);
    }

    @Test
    public void getLastPolicyUpdateEmitsTheLatestUpdateDate() {
        final long today = System.currentTimeMillis();
        final long yesterday = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
        final TestObserver<Long> observer = new TestObserver<>();

        insertCompletedDownload(yesterday);
        insertCompletedDownload(today);

        storage.getLastPolicyUpdate().subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(today);
        assertThat(observer.getOnCompletedEvents()).hasSize(1);
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
