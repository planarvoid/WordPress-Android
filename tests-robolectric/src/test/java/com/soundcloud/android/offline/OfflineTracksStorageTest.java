package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.PropellerWriteException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.observers.TestObserver;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(SoundCloudTestRunner.class)
public class OfflineTracksStorageTest extends StorageIntegrationTest {

    private static final Urn TRACK_1 = Urn.forTrack(123L);
    private static final Urn TRACK_2 = Urn.forTrack(456L);
    @Mock private DateProvider dateProvider;

    private OfflineTracksStorage storage;
    private TestObserver<List<Urn>> observer;

    @Before
    public void setup() {
        storage = new OfflineTracksStorage(propeller(), propellerRx(), dateProvider);
        observer = new TestObserver<>();
    }

    @Test
    public void loadsOfflineLikesOrderedByLikeDate() {
        final Urn track1 = insertOfflineLikeDownloadCompleted(100);
        final Urn track2 = insertOfflineLikeDownloadCompleted(200);

        storage.likesUrns().subscribe(observer);

        expect(observer.getOnNextEvents().get(0)).toContainExactly(track2, track1);
    }

    @Test
    public void doesNotLoadOfflineLikesPendingRemoval() {
        testFixtures().insertTrackDownloadPendingRemoval(TRACK_1, new Date(200).getTime());

        storage.likesUrns().subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0)).toBeEmpty();
    }

    @Test
    public void doesNotLoadNonOfflineLike() {
        testFixtures().insertLikedTrack(new Date(100));

        storage.likesUrns().subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0)).toBeEmpty();
    }

    @Test
    public void loadsOfflineLikedTracksPendingDownload() {
        insertOfflineLikeDownloadCompleted(100);
        final Urn track1 = insertOfflineLikePendingDownload(200);
        final Urn track2 = insertOfflineLikePendingDownload(300);

        storage.pendingLikedTracksUrns().subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0)).toContainExactly(track1, track2);
    }

    @Test
    public void loadsOfflineTracksFromAPlaylist() {
        final Urn playlistUrn = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();
        final Urn trackUrn1 = insertOfflinePlaylistTrack(playlistUrn, 0);
        final Urn trackUrn2 = insertOfflinePlaylistTrack(playlistUrn, 1);

        storage.playlistTrackUrns(playlistUrn).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0)).toContainExactly(trackUrn1, trackUrn2);
    }

    @Test
    public void doesNotLoadOfflineTracksFromAPlaylistMarkedForRemoval() {
        final Urn playlistUrn = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();
        final ApiTrack track = testFixtures().insertPlaylistTrack(playlistUrn, 0);
        testFixtures().insertTrackDownloadPendingRemoval(track.getUrn(), new Date(200).getTime());

        storage.playlistTrackUrns(playlistUrn).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0)).toBeEmpty();
    }

    @Test
    public void loadsOfflinePendingTrackFromAPlaylist() {
        final Urn playlistUrn = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();
        insertOfflinePlaylistTrack(playlistUrn, 0);
        final Urn trackUrn1 = insertOfflinePlaylistTrackPendingDownload(playlistUrn, 1);
        final Urn trackUrn2 = insertOfflinePlaylistTrackPendingDownload(playlistUrn, 2);

        storage.pendingPlaylistTracksUrns(playlistUrn).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0)).toContainExactly(trackUrn1, trackUrn2);
    }

    @Test
    public void getTracksToRemoveReturnsTrackPendingRemovalSinceAtLeast3Minutes() {
        final Date now = new Date();
        final Date fourMinutesAgo = new Date(now.getTime() - TimeUnit.MINUTES.toMillis(4));
        when(dateProvider.getCurrentDate()).thenReturn(now);

        testFixtures().insertTrackDownloadPendingRemoval(TRACK_1, now.getTime());
        testFixtures().insertTrackDownloadPendingRemoval(TRACK_2, fourMinutesAgo.getTime());

        storage.getTracksToRemove().subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0)).toContainExactly(TRACK_2);
    }

    @Test
    public void updatesDownloadTracksWithDownloadResults() throws PropellerWriteException {
        final DownloadResult downloadResult = DownloadResult.success(new DownloadRequest(TRACK_1, 12345L));
        testFixtures().insertTrackPendingDownload(TRACK_1, 100L);

        storage.storeCompletedDownload(downloadResult);

        databaseAssertions().assertDownloadResultsInserted(downloadResult);
    }

    @Test
    public void resetUnavailableAtWhenDownloaded() {
        testFixtures().insertUnavailableTrackDownload(TRACK_1, 100L);

        final DownloadResult downloadResult = DownloadResult.success(new DownloadRequest(TRACK_1, 12345L));
        storage.storeCompletedDownload(downloadResult);

        databaseAssertions().assertDownloadIsAvailable(TRACK_1);
    }

    @Test
    public void markTrackAsUnavailable() throws Exception {
        final Date now = new Date();
        testFixtures().insertTrackPendingDownload(TRACK_1, 100L);
        when(dateProvider.getCurrentDate()).thenReturn(now);

        storage.markTrackAsUnavailable(TRACK_1);

        databaseAssertions().assertTrackIsUnavailable(TRACK_1, now.getTime());
    }

    @Test
    public void getLastPolicyUpdateDoesNotEmitWhenNoPolicy() {
        final TestObserver<Long> observer = new TestObserver<>();

        storage.getLastPolicyUpdate().subscribe(observer);

        expect(observer.getOnNextEvents()).toBeEmpty();
        expect(observer.getOnCompletedEvents()).toNumber(1);
    }

    @Test
    public void getLastPolicyUpdateDoesNotEmitWhenNoTrackDownloadEntry() {
        final TestObserver<Long> observer = new TestObserver<>();
        insertTrackWithPolicy(System.currentTimeMillis());

        storage.getLastPolicyUpdate().subscribe(observer);

        expect(observer.getOnNextEvents()).toBeEmpty();
        expect(observer.getOnCompletedEvents()).toNumber(1);
    }

    @Test
    public void getLastPolicyUpdateEmitsTheLatestUpdateDate() {
        final long today = System.currentTimeMillis();
        final long yesterday = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
        final TestObserver<Long> observer = new TestObserver<>();

        insertCompletedDownload(yesterday);
        insertCompletedDownload(today);

        storage.getLastPolicyUpdate().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(today);
        expect(observer.getOnCompletedEvents()).toNumber(1);
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
