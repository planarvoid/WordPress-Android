package com.soundcloud.android.offline;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.creatorOptOutRequest;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.downloadRequestFromPlaylists;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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

        ApiTrack apiTrack = insertSyncableLikedTrack(100L);

        final Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).containsExactly(downloadRequestFromPlaylists(apiTrack, true, Collections.<Urn>emptyList()));
    }

    @Test
    public void returnsOfflinePlaylistTracksAsPendingDownloads() throws Exception {
        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack track1 = insertSyncablePlaylistTrack(playlist, 0);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).containsExactly(
                downloadRequestFromPlaylists(track1, false, Collections.singletonList(playlist.getUrn())));
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

        final ApiTrack apiTrack1 = insertSyncableLikedTrack(100);
        final ApiTrack apiTrack2 = insertSyncableLikedTrack(200);
        final ApiTrack apiTrack3 = insertSyncableLikedTrack(300);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).containsExactly(
                downloadRequestFromPlaylists(apiTrack3, true, Collections.<Urn>emptyList()),
                downloadRequestFromPlaylists(apiTrack2, true, Collections.<Urn>emptyList()),
                downloadRequestFromPlaylists(apiTrack1, true, Collections.<Urn>emptyList())
        );
    }

    @Test
    public void returnsOfflinePlaylistTracksOrderedByPosition() throws Exception {
        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();

        final ApiTrack playlistTrack1 = insertSyncablePlaylistTrack(playlist, 1);
        final ApiTrack playlistTrack0 = insertSyncablePlaylistTrack(playlist, 0);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).containsExactly(
                downloadRequestFromPlaylists(playlistTrack0, false, Collections.singletonList(playlist.getUrn())),
                downloadRequestFromPlaylists(playlistTrack1, false, Collections.singletonList(playlist.getUrn()))
        );
    }

    @Test
    public void returnsOfflinePlaylistsOrderedByPlaylistCreationDate() throws Exception {
        ApiUser user = testFixtures().insertUser();

        final ApiPlaylist apiPlaylist2 = testFixtures().insertPlaylistWithCreationDate(user, new Date(100));
        testFixtures().insertPlaylistMarkedForOfflineSync(apiPlaylist2);
        final ApiTrack playlistTrack2 = insertSyncablePlaylistTrack(apiPlaylist2, 0);

        final ApiPlaylist apiPlaylist1 = testFixtures().insertPlaylistWithCreationDate(user, new Date(20023094823L));
        testFixtures().insertPlaylistMarkedForOfflineSync(apiPlaylist1);
        final ApiTrack playlistTrack1 = insertSyncablePlaylistTrack(apiPlaylist1, 0);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).containsExactly(
                downloadRequestFromPlaylists(playlistTrack1, false, Collections.singletonList(apiPlaylist1.getUrn())),
                downloadRequestFromPlaylists(playlistTrack2, false, Collections.singletonList(apiPlaylist2.getUrn()))
        );
    }

    @Test
    public void returnsOfflinePlaylistTracksBeforeLikes() throws Exception {
        enableOfflineLikes();

        final ApiTrack apiTrack = insertSyncableLikedTrack(100);

        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack playlistTrack = insertSyncablePlaylistTrack(playlist, 0);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).containsExactly(
                downloadRequestFromPlaylists(playlistTrack, false, Collections.singletonList(playlist.getUrn())),
                downloadRequestFromPlaylists(apiTrack, true, Collections.<Urn>emptyList())
        );
    }

    @Test
    public void doesNotReturnDuplicatedDownloadRequests() throws Exception {
        enableOfflineLikes();

        final ApiTrack apiTrack = insertSyncableLikedTrack(100);

        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        testFixtures().insertPlaylistTrack(playlist.getUrn(), apiTrack.getUrn(), 0);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).containsExactly(
                downloadRequestFromPlaylists(apiTrack, true, Collections.singletonList(playlist.getUrn())));
    }

    @Test
    public void doesNotReturnTracksWithoutPolicy() {
        testFixtures().insertLikedTrack(new Date(100));

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).isEmpty();
    }

    @Test
    public void doesNotReturnBlockedTacks() {
        insertCreatorOptOutLikedTrack(100);

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
        ApiTrack apiTrack = testFixtures().insertLikedTrackPendingRemoval(new Date(0), new Date(10));
        testFixtures().insertPolicyAllow(apiTrack.getUrn(), NOW);

        final Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).isEmpty();
    }

    @Test
    public void doesNotIncludeTracksRemovedFromAPlaylist() {
        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack playlistTrack1 = testFixtures().insertPlaylistTrackPendingRemoval(playlist, 1, new Date(NOW));
        testFixtures().insertPolicyAllow(playlistTrack1.getUrn(), NOW);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline).isEmpty();
    }

    @Test
    public void singleCreatorOptOutTrackDoesNotInfluenceLikesCollectionState() {
        enableOfflineLikes();

        final ApiTrack creatorOptOut = insertCreatorOptOutLikedTrack(200);
        final ApiTrack syncable = insertSyncableLikedTrack(100);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline)
                .containsExactly(
                        creatorOptOutRequest(creatorOptOut, false, Collections.<Urn>emptyList()),
                        downloadRequestFromPlaylists(syncable, true, Collections.<Urn>emptyList()));
    }

    @Test
    public void creatorOptOutInfluencesLikesCollectionStateWhenAllLikedTracksAreOptedOut() {
        enableOfflineLikes();

        final ApiTrack creatorOptOut1 = insertCreatorOptOutLikedTrack(200);
        final ApiTrack creatorOptOut2 = insertCreatorOptOutLikedTrack(100);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline)
                .containsExactly(
                        creatorOptOutRequest(creatorOptOut1, true, Collections.<Urn>emptyList()),
                        creatorOptOutRequest(creatorOptOut2, true, Collections.<Urn>emptyList()));
    }

    @Test
    public void singleCreatorOptOutDoesNotInfluencePlaylistCollectionState() {
        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack syncable = insertSyncablePlaylistTrack(playlist, 0);
        final ApiTrack creatorOptOut = insertCreatorOptOutPlaylistTrack(playlist, 1);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline)
                .containsExactly(
                        downloadRequestFromPlaylists(syncable, false, Collections.singletonList(playlist.getUrn())),
                        creatorOptOutRequest(creatorOptOut, false, Collections.<Urn>emptyList()));
    }

    @Test
    public void creatorOptOutInfluencesPlaylistCollectionStateWhenAllTracksAreOptedOut() {
        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack creatorOptOut1 = insertCreatorOptOutPlaylistTrack(playlist, 0);
        final ApiTrack creatorOptOut2 = insertCreatorOptOutPlaylistTrack(playlist, 1);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline)
                .containsExactly(
                        creatorOptOutRequest(creatorOptOut1, false, Collections.singletonList(playlist.getUrn())),
                        creatorOptOutRequest(creatorOptOut2, false, Collections.singletonList(playlist.getUrn())));
    }

    @Test
    public void creatorOptOutInfluencesOtherPlaylistCollectionState() {
        ApiUser user = testFixtures().insertUser();

        final ApiPlaylist playlist1 = testFixtures().insertPlaylistWithCreationDate(user, new Date(100));
        testFixtures().insertPlaylistMarkedForOfflineSync(playlist1);
        final ApiTrack syncedTrack = insertSyncablePlaylistTrack(playlist1, 0);

        final ApiPlaylist playlist2 = testFixtures().insertPlaylistWithCreationDate(user, new Date(200));
        testFixtures().insertPlaylistMarkedForOfflineSync(playlist2);
        final ApiTrack syncedTrack2 = insertSyncablePlaylistTrack(playlist2, 0);
        final ApiTrack creatorOptOut2 = insertCreatorOptOutPlaylistTrack(playlist2, 1);

        Collection<DownloadRequest> toBeOffline = command.call(null);

        assertThat(toBeOffline)
                .containsExactly(
                        downloadRequestFromPlaylists(syncedTrack2, false, Collections.singletonList(playlist2.getUrn())),
                        creatorOptOutRequest(creatorOptOut2, false, Collections.<Urn>emptyList()),
                        downloadRequestFromPlaylists(syncedTrack, false, Collections.singletonList(playlist1.getUrn())));
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

    private ApiTrack insertCreatorOptOutLikedTrack(long likedAt) {
        final ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(likedAt));
        testFixtures().insertPolicyBlock(apiTrack.getUrn());
        return apiTrack;
    }

    private ApiTrack insertSyncableLikedTrack(long likedAt) {
        final ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(likedAt));
        testFixtures().insertPolicyAllow(apiTrack.getUrn(), NOW);
        return apiTrack;
    }

    private ApiTrack insertSyncablePlaylistTrack(ApiPlaylist playlist, int trackNum) {
        final ApiTrack playlistTrack = testFixtures().insertPlaylistTrack(playlist, trackNum);
        testFixtures().insertPolicyAllow(playlistTrack.getUrn(), NOW);
        return playlistTrack;
    }

    private ApiTrack insertCreatorOptOutPlaylistTrack(ApiPlaylist playlist, int trackNum) {
        final ApiTrack playlistTrack = testFixtures().insertPlaylistTrack(playlist, trackNum);
        testFixtures().insertPolicyBlock(playlistTrack.getUrn());
        return playlistTrack;
    }
}