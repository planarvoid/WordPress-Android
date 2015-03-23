package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.offline.DownloadRequest;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class LoadPrioritizedPendingDownloadsCommandTest extends StorageIntegrationTest {

    private LoadPrioritizedPendingDownloadsCommand command;
    private ApiTrack apiTrack;

    @Before
    public void setUp() {
        command = new LoadPrioritizedPendingDownloadsCommand(propeller());
        apiTrack = testFixtures().insertLikedTrack(new Date(10));
    }

    @Test
    public void returnsLikedTracksAsPendingDownloads() throws Exception {
        testFixtures().insertTrackPendingDownload(apiTrack.getUrn(), 100);

        List<DownloadRequest> pending = command.call();

        expect(pending).toNumber(1);
        expectDownloadRequestMatchApiTrack(pending.get(0), apiTrack);
    }

    @Test
    public void returnsOfflinePlaylistTracksAsPendingDownloads() throws Exception {
        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack track1 = testFixtures().insertPlaylistTrack(playlist, 0);
        testFixtures().insertTrackPendingDownload(track1.getUrn(), 100);

        List<DownloadRequest> pending = command.call();
        expect(pending).toNumber(1);
        expectDownloadRequestMatchApiTrack(pending.get(0), track1);
    }

    @Test
    public void returnsEmptyListWhenAllDownloadsCompleted() throws Exception {
        testFixtures().insertCompletedTrackDownload(apiTrack.getUrn(), 100);

        List<DownloadRequest> pending = command.call();
        expect(pending).toBeEmpty();
    }

    @Test
    public void doesNotReturnTrackDownloadsPendingRemoval() throws Exception {
        testFixtures().insertTrackDownloadPendingRemoval(apiTrack.getUrn(), 100);

        List<DownloadRequest> pending = command.call();
        expect(pending).toBeEmpty();
    }

    @Test
    public void doesNotReturnDownloadedTrackPendingRemoval() throws Exception {
        testFixtures().insertTrackDownloadPendingRemoval(apiTrack.getUrn(), 100);
        database().execSQL("UPDATE TrackDownloads SET downloaded_at=100"
                + " WHERE _id=" + apiTrack.getUrn().getNumericId());

        List<DownloadRequest> pending = command.call();
        expect(pending).toBeEmpty();
    }

    @Test
    public void returnsLikesPendingDownloadOrderedByLikeDate() throws Exception {
        ApiTrack apiTrack1 = testFixtures().insertLikedTrackPendingDownload(new Date(100));
        ApiTrack apiTrack2 = testFixtures().insertLikedTrackPendingDownload(new Date(200));
        ApiTrack apiTrack3 = testFixtures().insertLikedTrackPendingDownload(new Date(300));

        List<DownloadRequest> pending = command.call();
        expectDownloadRequestMatchApiTrack(pending.get(0), apiTrack3);
        expectDownloadRequestMatchApiTrack(pending.get(1), apiTrack2);
        expectDownloadRequestMatchApiTrack(pending.get(2), apiTrack1);
    }

    @Test
    public void returnsOfflinePlaylistTracksOrderedByPosition() throws Exception {
        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();

        final ApiTrack playlistTrack1 = testFixtures().insertPlaylistTrack(playlist, 1);
        testFixtures().insertTrackPendingDownload(playlistTrack1.getUrn(), 100);

        final ApiTrack playlistTrack0 = testFixtures().insertPlaylistTrack(playlist, 0);
        testFixtures().insertTrackPendingDownload(playlistTrack0.getUrn(), 100);

        List<DownloadRequest> pending = command.call();
        expectDownloadRequestMatchApiTrack(pending.get(0), playlistTrack0);
        expectDownloadRequestMatchApiTrack(pending.get(1), playlistTrack1);
    }

    @Test
    public void returnsOfflinePlaylistsOrderedByPlaylistCreationDate() throws Exception {
        ApiUser user = testFixtures().insertUser();

        final ApiPlaylist apiPlaylist2 = testFixtures().insertPlaylistWithCreationDate(user, new Date(100));
        final ApiTrack playlistTrack2 = testFixtures().insertPlaylistTrack(apiPlaylist2, 0);
        testFixtures().insertTrackPendingDownload(playlistTrack2.getUrn(), 100);
        testFixtures().insertPlaylistMarkedForOfflineSync(apiPlaylist2);

        final ApiPlaylist apiPlaylist1 = testFixtures().insertPlaylistWithCreationDate(user, new Date(20023094823L));
        final ApiTrack playlistTrack1 = testFixtures().insertPlaylistTrack(apiPlaylist1, 0);
        testFixtures().insertTrackPendingDownload(playlistTrack1.getUrn(), 100);
        testFixtures().insertPlaylistMarkedForOfflineSync(apiPlaylist1);

        List<DownloadRequest> pending = command.call();

        expect(pending).toNumber(2);
        expectDownloadRequestMatchApiTrack(pending.get(0), playlistTrack1);
        expectDownloadRequestMatchApiTrack(pending.get(1), playlistTrack2);
    }

    @Test
    public void returnsOfflinePlaylistTracksBeforeLikes() throws Exception {
        final ApiTrack otherTrack = testFixtures().insertLikedTrackPendingDownload(new Date(100));

        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack playlistTrack = testFixtures().insertPlaylistTrack(playlist, 0);
        testFixtures().insertTrackPendingDownload(playlistTrack.getUrn(), 100);

        List<DownloadRequest> pending = command.call();
        expectDownloadRequestMatchApiTrack(pending.get(0), playlistTrack);
        expectDownloadRequestMatchApiTrack(pending.get(1), otherTrack);
    }

    @Test
    public void doesNotReturnDuplicatedDownloadRequests() throws Exception {
        final ApiTrack otherTrack = testFixtures().insertLikedTrackPendingDownload(new Date(100));

        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        testFixtures().insertPlaylistTrack(playlist.getUrn(), otherTrack.getUrn(), 0);

        List<DownloadRequest> pending = command.call();

        expect(pending).toNumber(1);
        expectDownloadRequestMatchApiTrack(pending.get(0), otherTrack);
    }

    private void expectDownloadRequestMatchApiTrack(DownloadRequest request, ApiTrack track) {
        expect(request.urn).toEqual(track.getUrn());
        expect(request.fileUrl).toEqual(track.getStreamUrl());
    }

}