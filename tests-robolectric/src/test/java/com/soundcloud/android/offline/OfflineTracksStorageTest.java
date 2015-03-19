package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.observers.TestObserver;

import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class OfflineTracksStorageTest extends StorageIntegrationTest {

    private OfflineTracksStorage storage;
    private TestObserver<List<Urn>> observer;

    @Before
    public void setup() {
        storage = new OfflineTracksStorage(testScheduler());
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
        testFixtures().insertTrackDownloadPendingRemoval(Urn.forTrack(234L), new Date(200).getTime());

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

    private Urn insertOfflinePlaylistTrack(Urn playlist, int position) {
        final ApiTrack track = testFixtures().insertPlaylistTrack(playlist, position);
        testFixtures().insertCompletedTrackDownload(track.getUrn(), 100);
        return track.getUrn();
    }

    private Urn insertOfflinePlaylistTrackPendingDownload(Urn playlist, int position) {
        final ApiTrack track = testFixtures().insertPlaylistTrack(playlist, position);
        testFixtures().insertRequestedTrackDownload(track.getUrn(), 100);
        return track.getUrn();
    }

    private Urn insertOfflineLikeDownloadCompleted(long likedAt) {
        final ApiTrack track = testFixtures().insertLikedTrack(new Date(likedAt));
        testFixtures().insertCompletedTrackDownload(track.getUrn(), 100, 200);

        return track.getUrn();
    }

    private Urn insertOfflineLikePendingDownload(long likedAt) {
        final ApiTrack track = testFixtures().insertLikedTrack(new Date(likedAt));
        testFixtures().insertRequestedTrackDownload(track.getUrn(), 100);

        return track.getUrn();
    }

}
