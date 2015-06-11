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
import java.util.concurrent.TimeUnit;

@RunWith(SoundCloudTestRunner.class)
public class LoadExpectedContentCommandTest extends StorageIntegrationTest {

    private LoadExpectedContentCommand command;

    private long NOW;
    @Mock private OfflineSettingsStorage settingsStorage;

    @Before
    public void setUp() {
        NOW = System.currentTimeMillis();
        when(settingsStorage.isOfflineLikedTracksEnabled()).thenReturn(true);
        command = new LoadExpectedContentCommand(propeller(), settingsStorage);
    }

    @Test
    public void returnsLikedTracksAsPendingDownloads() throws Exception {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(10));
        Urn urn = apiTrack.getUrn();
        testFixtures().insertPolicyAllow(urn, NOW);

        final Collection<DownloadRequest> toBeOffline = command.call(null);

        expect(toBeOffline).toContainExactly(new DownloadRequest(apiTrack.getUrn(), apiTrack.getDuration(), true, Collections.<Urn>emptyList()));
    }

    @Test
    public void returnsOfflinePlaylistTracksAsPendingDownloads() throws Exception {
        when(settingsStorage.isOfflineLikedTracksEnabled()).thenReturn(false);
        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack track1 = testFixtures().insertPlaylistTrack(playlist, 0);
        Urn urn = track1.getUrn();
        testFixtures().insertPolicyAllow(urn, NOW);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        expect(toBeOffline).toContainExactly(new DownloadRequest(track1.getUrn(), track1.getDuration(), false, Arrays.asList(playlist.getUrn())));
    }

    @Test
    public void returnsEmptyListWhenAllDownloadsCompleted() throws Exception {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(10));
        testFixtures().insertCompletedTrackDownload(apiTrack.getUrn(), 0, 100);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        expect(toBeOffline).toBeEmpty();
    }

    @Test
    public void doesNotReturnTrackDownloadsPendingRemoval() throws Exception {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(10));
        testFixtures().insertTrackDownloadPendingRemoval(apiTrack.getUrn(), 100);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        expect(toBeOffline).toBeEmpty();
    }

    @Test
    public void doesNotReturnDownloadedTrackPendingRemoval() throws Exception {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(10));
            testFixtures().insertTrackDownloadPendingRemoval(apiTrack.getUrn(), 100);
        database().execSQL("UPDATE TrackDownloads SET downloaded_at=100"
                + " WHERE _id=" + apiTrack.getUrn().getNumericId());

        Collection<DownloadRequest> toBeOffline = command.call(null);

        expect(toBeOffline).toBeEmpty();
    }

    @Test
    public void returnsLikesPendingDownloadOrderedByLikeDate() throws Exception {
        final ApiTrack apiTrack1 = testFixtures().insertLikedTrack(new Date(100));
        testFixtures().insertPolicyAllow(apiTrack1.getUrn(), NOW);

        final ApiTrack apiTrack2 = testFixtures().insertLikedTrack(new Date(200));
        testFixtures().insertPolicyAllow(apiTrack2.getUrn(), NOW);

        final ApiTrack apiTrack3 = testFixtures().insertLikedTrack(new Date(300));
        testFixtures().insertPolicyAllow(apiTrack3.getUrn(), NOW);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        expect(toBeOffline).toContainExactly(
                new DownloadRequest(apiTrack3.getUrn(), apiTrack1.getDuration(), true, Collections.<Urn>emptyList()),
                new DownloadRequest(apiTrack2.getUrn(), apiTrack2.getDuration(), true, Collections.<Urn>emptyList()),
                new DownloadRequest(apiTrack1.getUrn(), apiTrack3.getDuration(), true, Collections.<Urn>emptyList())
        );
    }

    @Test
    public void returnsOfflinePlaylistTracksOrderedByPosition() throws Exception {
        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();

        final ApiTrack playlistTrack1 = testFixtures().insertPlaylistTrack(playlist, 1);
        Urn urn1 = playlistTrack1.getUrn();
        testFixtures().insertPolicyAllow(urn1, NOW);

        final ApiTrack playlistTrack0 = testFixtures().insertPlaylistTrack(playlist, 0);
        Urn urn2 = playlistTrack0.getUrn();
        testFixtures().insertPolicyAllow(urn2, NOW);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        expect(toBeOffline).toContainExactly(
                new DownloadRequest(playlistTrack0.getUrn(), playlistTrack0.getDuration(), false, Arrays.asList(playlist.getUrn())),
                new DownloadRequest(playlistTrack1.getUrn(), playlistTrack1.getDuration(), false, Arrays.asList(playlist.getUrn()))
        );
    }

    @Test
    public void returnsOfflinePlaylistsOrderedByPlaylistCreationDate() throws Exception {
        ApiUser user = testFixtures().insertUser();

        final ApiPlaylist apiPlaylist2 = testFixtures().insertPlaylistWithCreationDate(user, new Date(100));
        final ApiTrack playlistTrack2 = testFixtures().insertPlaylistTrack(apiPlaylist2, 0);
        Urn urn1 = playlistTrack2.getUrn();
        testFixtures().insertPolicyAllow(urn1, NOW);
        testFixtures().insertPlaylistMarkedForOfflineSync(apiPlaylist2);

        final ApiPlaylist apiPlaylist1 = testFixtures().insertPlaylistWithCreationDate(user, new Date(20023094823L));
        final ApiTrack playlistTrack1 = testFixtures().insertPlaylistTrack(apiPlaylist1, 0);
        Urn urn2 = playlistTrack1.getUrn();
        testFixtures().insertPolicyAllow(urn2, NOW);
        testFixtures().insertPlaylistMarkedForOfflineSync(apiPlaylist1);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        expect(toBeOffline).toContainExactly(
                new DownloadRequest(playlistTrack1.getUrn(), playlistTrack1.getDuration(), false, Arrays.asList(apiPlaylist1.getUrn())),
                new DownloadRequest(playlistTrack2.getUrn(), playlistTrack2.getDuration(), false, Arrays.asList(apiPlaylist2.getUrn()))
        );
    }

    @Test
    public void returnsOfflinePlaylistTracksBeforeLikes() throws Exception {
        final ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(100));
        testFixtures().insertPolicyAllow(apiTrack.getUrn(), NOW);

        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack playlistTrack = testFixtures().insertPlaylistTrack(playlist, 0);
        Urn urn = playlistTrack.getUrn();
        testFixtures().insertPolicyAllow(urn, NOW);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        expect(toBeOffline).toContainExactly(
                new DownloadRequest(playlistTrack.getUrn(), playlistTrack.getDuration(), false, Arrays.asList(playlist.getUrn())),
                new DownloadRequest(apiTrack.getUrn(), apiTrack.getDuration(), true, Collections.<Urn>emptyList())
        );
    }

    @Test
    public void doesNotReturnDuplicatedDownloadRequests() throws Exception {
        final ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(100));
        testFixtures().insertPolicyAllow(apiTrack.getUrn(), NOW);

        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        testFixtures().insertPlaylistTrack(playlist.getUrn(), apiTrack.getUrn(), 0);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        expect(toBeOffline).toContainExactly(new DownloadRequest(apiTrack.getUrn(), apiTrack.getDuration(), true, Arrays.asList(playlist.getUrn())));
    }

    @Test
    public void doesNotReturnTracksWithoutPolicy() {
        testFixtures().insertLikedTrack(new Date(100));

        Collection<DownloadRequest> toBeOffline = command.call(null);

        expect(toBeOffline).toBeEmpty();
    }

    @Test
    public void doesNotReturnBlockedTacks() {
        final ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(100));
        testFixtures().insertPolicyBlock(apiTrack.getUrn());

        Collection<DownloadRequest> toBeOffline = command.call(null);

        expect(toBeOffline).toBeEmpty();
    }

    @Test
    public void doesNotReturnLikedTrackWhenPolicyUpdateHappenedAfterTheLast30Days() {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(10));
        testFixtures().insertPolicyAllow(apiTrack.getUrn(), NOW - TimeUnit.DAYS.toMillis(30));

        final Collection<DownloadRequest> toBeOffline = command.call(null);

        expect(toBeOffline).toBeEmpty();
    }

    @Test
    public void doesNotIncludeReturnRemovedLikes() {
        ApiTrack apiTrack = testFixtures().insertLikedTrackPendingRemoval(new Date(10));
        testFixtures().insertPolicyAllow(apiTrack.getUrn(), NOW);

        final Collection<DownloadRequest> toBeOffline = command.call(null);

        expect(toBeOffline).toBeEmpty();
    }

    @Test
    public void doesNotIncludeTracksRemovedFromAPlaylist() {
        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack playlistTrack1 = testFixtures().insertPlaylistTrackPendingRemoval(playlist, 1, new Date(NOW));
        Urn urn1 = playlistTrack1.getUrn();
        testFixtures().insertPolicyAllow(urn1, NOW);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        expect(toBeOffline).toBeEmpty();
    }

    @Test
    public void doesNotReturnOfflinePlaylistTracksWhenPolicyUpdateHappenedAfterTheLast30Days() throws Exception {
        when(settingsStorage.isOfflineLikedTracksEnabled()).thenReturn(false);
        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack track1 = testFixtures().insertPlaylistTrack(playlist, 0);
        Urn urn = track1.getUrn();
        testFixtures().insertPolicyAllow(urn, NOW - TimeUnit.DAYS.toMillis(30));

        Collection<DownloadRequest> toBeOffline = command.call(null);

        expect(toBeOffline).toBeEmpty();
    }
}