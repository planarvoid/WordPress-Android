package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LoadExpectedContentCommandTest extends StorageIntegrationTest {

    private LoadExpectedContentCommand command;

    private long NOW;

    @Before
    public void setUp() {
        NOW = System.currentTimeMillis();
        command = new LoadExpectedContentCommand(propeller());
    }

    @Test
    public void returnsLikedTracksAsPendingDownloads() throws Exception {
        enableOfflineLikes();

        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(10));
        testFixtures().insertPolicyAllow(apiTrack.getUrn(), NOW);

        final Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).containsExactly(downloadRequest(apiTrack, true, Collections.<Urn>emptyList()));
    }

    @Test
    public void returnsOfflinePlaylistTracksAsPendingDownloads() throws Exception {
        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack track1 = testFixtures().insertPlaylistTrack(playlist, 0);
        Urn urn = track1.getUrn();
        testFixtures().insertPolicyAllow(urn, NOW);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).containsExactly(
                downloadRequest(track1, false, Collections.singletonList(playlist.getUrn())));
    }

    @Test
    public void returnsEmptyListWhenAllDownloadsCompleted() throws Exception {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(10));
        testFixtures().insertCompletedTrackDownload(apiTrack.getUrn(), 0, 100);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).isEmpty();
    }

    @Test
    public void doesNotReturnTrackDownloadsPendingRemoval() throws Exception {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(10));
        testFixtures().insertTrackDownloadPendingRemoval(apiTrack.getUrn(), 100);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).isEmpty();
    }

    @Test
    public void doesNotReturnDownloadedTrackPendingRemoval() throws Exception {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(10));
        testFixtures().insertTrackDownloadPendingRemoval(apiTrack.getUrn(), 100);
        database().execSQL("UPDATE TrackDownloads SET downloaded_at=100"
                + " WHERE _id=" + apiTrack.getUrn().getNumericId());

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).isEmpty();
    }

    @Test
    public void returnsLikesPendingDownloadOrderedByLikeDate() throws Exception {
        enableOfflineLikes();

        final ApiTrack apiTrack1 = testFixtures().insertLikedTrack(new Date(100));
        testFixtures().insertPolicyAllow(apiTrack1.getUrn(), NOW);

        final ApiTrack apiTrack2 = testFixtures().insertLikedTrack(new Date(200));
        testFixtures().insertPolicyAllow(apiTrack2.getUrn(), NOW);

        final ApiTrack apiTrack3 = testFixtures().insertLikedTrack(new Date(300));
        testFixtures().insertPolicyAllow(apiTrack3.getUrn(), NOW);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).containsExactly(
                downloadRequest(apiTrack3, true, Collections.<Urn>emptyList()),
                downloadRequest(apiTrack2, true, Collections.<Urn>emptyList()),
                downloadRequest(apiTrack1, true, Collections.<Urn>emptyList())
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

        assertThat(toBeOffline).containsExactly(
                downloadRequest(playlistTrack0, false, Collections.singletonList(playlist.getUrn())),
                downloadRequest(playlistTrack1, false, Collections.singletonList(playlist.getUrn()))
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

        assertThat(toBeOffline).containsExactly(
                downloadRequest(playlistTrack1, false, Collections.singletonList(apiPlaylist1.getUrn())),
                downloadRequest(playlistTrack2, false, Collections.singletonList(apiPlaylist2.getUrn()))
        );
    }

    @Test
    public void returnsOfflinePlaylistTracksBeforeLikes() throws Exception {
        enableOfflineLikes();

        final ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(100));
        testFixtures().insertPolicyAllow(apiTrack.getUrn(), NOW);

        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack playlistTrack = testFixtures().insertPlaylistTrack(playlist, 0);
        Urn urn = playlistTrack.getUrn();
        testFixtures().insertPolicyAllow(urn, NOW);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).containsExactly(
                downloadRequest(playlistTrack, false, Collections.singletonList(playlist.getUrn())),
                downloadRequest(apiTrack, true, Collections.<Urn>emptyList())
        );
    }

    @Test
    public void doesNotReturnDuplicatedDownloadRequests() throws Exception {
        enableOfflineLikes();

        final ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(100));
        testFixtures().insertPolicyAllow(apiTrack.getUrn(), NOW);

        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        testFixtures().insertPlaylistTrack(playlist.getUrn(), apiTrack.getUrn(), 0);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).containsExactly(
                downloadRequest(apiTrack, true, Collections.singletonList(playlist.getUrn())));
    }

    @Test
    public void doesNotReturnTracksWithoutPolicy() {
        testFixtures().insertLikedTrack(new Date(100));

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).isEmpty();
    }

    @Test
    public void doesNotReturnBlockedTacks() {
        final ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(100));
        testFixtures().insertPolicyBlock(apiTrack.getUrn());

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).isEmpty();
    }

    @Test
    public void doesNotReturnLikedTrackWhenPolicyUpdateHappenedAfterTheLast30Days() {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(10));
        testFixtures().insertPolicyAllow(apiTrack.getUrn(), NOW - TimeUnit.DAYS.toMillis(30));

        final Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).isEmpty();
    }

    @Test
    public void doesNotIncludeReturnRemovedLikes() {
        ApiTrack apiTrack = testFixtures().insertLikedTrackPendingRemoval(new Date(10));
        testFixtures().insertPolicyAllow(apiTrack.getUrn(), NOW);

        final Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).isEmpty();
    }

    @Test
    public void doesNotIncludeTracksRemovedFromAPlaylist() {
        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack playlistTrack1 = testFixtures().insertPlaylistTrackPendingRemoval(playlist, 1, new Date(NOW));
        Urn urn1 = playlistTrack1.getUrn();
        testFixtures().insertPolicyAllow(urn1, NOW);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).isEmpty();
    }

    @Test
    public void doesNotReturnOfflinePlaylistTracksWhenPolicyUpdateHappenedAfterTheLast30Days() throws Exception {
        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack track1 = testFixtures().insertPlaylistTrack(playlist, 0);
        Urn urn = track1.getUrn();
        testFixtures().insertPolicyAllow(urn, NOW - TimeUnit.DAYS.toMillis(30));

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).isEmpty();
    }

    private void enableOfflineLikes() {
        testFixtures().insertLikesMarkedForOfflineSync();
    }

    private DownloadRequest downloadRequest(ApiTrack track, boolean inLikes, List<Urn> inPlaylists) {
        return new DownloadRequest(track.getUrn(), track.getDuration(), track.getWaveformUrl(), track.isSyncable(), inLikes, inPlaylists);
    }
}