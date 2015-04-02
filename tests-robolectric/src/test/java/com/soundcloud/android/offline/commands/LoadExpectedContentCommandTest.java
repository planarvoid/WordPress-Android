package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadRequest;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class LoadExpectedContentCommandTest extends StorageIntegrationTest {

    private LoadExpectedContentCommand command;
    @Mock private OfflineSettingsStorage settingsStorage;

    @Before
    public void setUp() {
        when(settingsStorage.isOfflineLikedTracksEnabled()).thenReturn(true);
        command = new LoadExpectedContentCommand(propeller(), settingsStorage);
    }

    @Test
    public void returnsLikedTracksAsPendingDownloads() throws Exception {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(10));
        Urn urn = apiTrack.getUrn();
        testFixtures().insertTrackPendingDownload(urn, 100);
        testFixtures().insertPolicyAllow(urn);

        final Collection<DownloadRequest> pending = command.call(null);

        expect(pending).toContainExactly(new DownloadRequest(apiTrack.getUrn(), apiTrack.getStreamUrl(), true, Collections.<Urn>emptyList()));
    }

    @Test
    public void returnsOfflinePlaylistTracksAsPendingDownloads() throws Exception {
        when(settingsStorage.isOfflineLikedTracksEnabled()).thenReturn(false);
        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack track1 = testFixtures().insertPlaylistTrack(playlist, 0);
        Urn urn = track1.getUrn();
        testFixtures().insertTrackPendingDownload(urn, 100);
        testFixtures().insertPolicyAllow(urn);

        Collection<DownloadRequest> pending = command.call(null);

        expect(pending).toContainExactly(new DownloadRequest(track1.getUrn(), track1.getStreamUrl(), false, Arrays.asList(playlist.getUrn())));
    }

    @Test
    public void returnsEmptyListWhenAllDownloadsCompleted() throws Exception {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(10));
        testFixtures().insertCompletedTrackDownload(apiTrack.getUrn(), 100);

        Collection<DownloadRequest> pending = command.call(null);

        expect(pending).toBeEmpty();
    }

    @Test
    public void doesNotReturnTrackDownloadsPendingRemoval() throws Exception {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(10));
        testFixtures().insertTrackDownloadPendingRemoval(apiTrack.getUrn(), 100);

        Collection<DownloadRequest> pending = command.call(null);

        expect(pending).toBeEmpty();
    }

    @Test
    public void doesNotReturnDownloadedTrackPendingRemoval() throws Exception {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(10));
            testFixtures().insertTrackDownloadPendingRemoval(apiTrack.getUrn(), 100);
        database().execSQL("UPDATE TrackDownloads SET downloaded_at=100"
                + " WHERE _id=" + apiTrack.getUrn().getNumericId());

        Collection<DownloadRequest> pending = command.call(null);

        expect(pending).toBeEmpty();
    }

    @Test
    public void returnsLikesPendingDownloadOrderedByLikeDate() throws Exception {
        final ApiTrack apiTrack1 = testFixtures().insertLikedTrackPendingDownload(new Date(100));
        testFixtures().insertPolicyAllow(apiTrack1.getUrn());

        final ApiTrack apiTrack2 = testFixtures().insertLikedTrackPendingDownload(new Date(200));
        testFixtures().insertPolicyAllow(apiTrack2.getUrn());

        final ApiTrack apiTrack3 = testFixtures().insertLikedTrackPendingDownload(new Date(300));
        testFixtures().insertPolicyAllow(apiTrack3.getUrn());

        Collection<DownloadRequest> pending = command.call(null);

        expect(pending).toContainExactly(
                new DownloadRequest(apiTrack3.getUrn(), apiTrack1.getStreamUrl(), true, Collections.<Urn>emptyList()),
                new DownloadRequest(apiTrack2.getUrn(), apiTrack2.getStreamUrl(), true, Collections.<Urn>emptyList()),
                new DownloadRequest(apiTrack1.getUrn(), apiTrack3.getStreamUrl(), true, Collections.<Urn>emptyList())
        );
    }

    @Test
    public void returnsOfflinePlaylistTracksOrderedByPosition() throws Exception {
        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();

        final ApiTrack playlistTrack1 = testFixtures().insertPlaylistTrack(playlist, 1);
        Urn urn1 = playlistTrack1.getUrn();
        testFixtures().insertTrackPendingDownload(urn1, 100);
        testFixtures().insertPolicyAllow(urn1);

        final ApiTrack playlistTrack0 = testFixtures().insertPlaylistTrack(playlist, 0);
        Urn urn2 = playlistTrack0.getUrn();
        testFixtures().insertTrackPendingDownload(urn2, 100);
        testFixtures().insertPolicyAllow(urn2);

        Collection<DownloadRequest> pending = command.call(null);

        expect(pending).toContainExactly(
                new DownloadRequest(playlistTrack0.getUrn(), playlistTrack0.getStreamUrl(), false, Arrays.asList(playlist.getUrn())),
                new DownloadRequest(playlistTrack1.getUrn(), playlistTrack1.getStreamUrl(), false, Arrays.asList(playlist.getUrn()))
        );
    }

    @Test
    public void returnsOfflinePlaylistsOrderedByPlaylistCreationDate() throws Exception {
        ApiUser user = testFixtures().insertUser();

        final ApiPlaylist apiPlaylist2 = testFixtures().insertPlaylistWithCreationDate(user, new Date(100));
        final ApiTrack playlistTrack2 = testFixtures().insertPlaylistTrack(apiPlaylist2, 0);
        Urn urn1 = playlistTrack2.getUrn();
        testFixtures().insertTrackPendingDownload(urn1, 100);
        testFixtures().insertPolicyAllow(urn1);
        testFixtures().insertPlaylistMarkedForOfflineSync(apiPlaylist2);

        final ApiPlaylist apiPlaylist1 = testFixtures().insertPlaylistWithCreationDate(user, new Date(20023094823L));
        final ApiTrack playlistTrack1 = testFixtures().insertPlaylistTrack(apiPlaylist1, 0);
        Urn urn2 = playlistTrack1.getUrn();
        testFixtures().insertTrackPendingDownload(urn2, 100);
        testFixtures().insertPolicyAllow(urn2);
        testFixtures().insertPlaylistMarkedForOfflineSync(apiPlaylist1);

        Collection<DownloadRequest> pending = command.call(null);

        expect(pending).toContainExactly(
                new DownloadRequest(playlistTrack1.getUrn(), playlistTrack1.getStreamUrl(), false, Arrays.asList(apiPlaylist1.getUrn())),
                new DownloadRequest(playlistTrack2.getUrn(), playlistTrack2.getStreamUrl(), false, Arrays.asList(apiPlaylist2.getUrn()))
        );
    }

    @Test
    public void returnsOfflinePlaylistTracksBeforeLikes() throws Exception {
        final ApiTrack apiTrack = testFixtures().insertLikedTrackPendingDownload(new Date(100));
        testFixtures().insertPolicyAllow(apiTrack.getUrn());

        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack playlistTrack = testFixtures().insertPlaylistTrack(playlist, 0);
        Urn urn = playlistTrack.getUrn();
        testFixtures().insertTrackPendingDownload(urn, 100);
        testFixtures().insertPolicyAllow(urn);

        Collection<DownloadRequest> pending = command.call(null);

        expect(pending).toContainExactly(
                new DownloadRequest(playlistTrack.getUrn(), playlistTrack.getStreamUrl(), false, Arrays.asList(playlist.getUrn())),
                new DownloadRequest(apiTrack.getUrn(), apiTrack.getStreamUrl(), true, Collections.<Urn>emptyList())
        );
    }

    @Test
    public void doesNotReturnDuplicatedDownloadRequests() throws Exception {
        final ApiTrack apiTrack = testFixtures().insertLikedTrackPendingDownload(new Date(100));
        testFixtures().insertPolicyAllow(apiTrack.getUrn());

        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        testFixtures().insertPlaylistTrack(playlist.getUrn(), apiTrack.getUrn(), 0);

        Collection<DownloadRequest> pending = command.call(null);

        expect(pending).toContainExactly(new DownloadRequest(apiTrack.getUrn(), apiTrack.getStreamUrl(), true, Arrays.asList(playlist.getUrn())));
    }

    @Test
    public void doesNotReturnTracksWithoutPolicy() {
        testFixtures().insertLikedTrackPendingDownload(new Date(100));

        Collection<DownloadRequest> pending = command.call(null);

        expect(pending).toBeEmpty();
    }

    @Test
    public void doesNotReturnBlockedTacks() {
        final ApiTrack apiTrack = testFixtures().insertLikedTrackPendingDownload(new Date(100));
        testFixtures().insertPolicyBlock(apiTrack.getUrn());

        Collection<DownloadRequest> pending = command.call(null);

        expect(pending).toBeEmpty();
    }
}