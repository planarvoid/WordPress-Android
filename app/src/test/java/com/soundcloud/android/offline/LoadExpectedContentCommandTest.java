package com.soundcloud.android.offline;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.downloadRequestFromLikes;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.downloadRequestFromLikesAndPlaylists;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.downloadRequestFromPlaylists;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.likes.LikesStorage;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.LoadPlaylistTracksCommand;
import com.soundcloud.android.playlists.NewPlaylistMapper;
import com.soundcloud.android.playlists.PlaylistStorage;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.TrackStorage;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class LoadExpectedContentCommandTest extends StorageIntegrationTest {

    private final OfflineContentStorage offlineContentStorage = offlineContentStorageWithLikesEnabled();
    private LoadExpectedContentCommand command;

    private long NOW;

    @Before
    public void setUp() {
        NOW = System.currentTimeMillis();
        command = new LoadExpectedContentCommand(
                new LikesStorage(propellerRxV2()),
                new TrackStorage(propellerRxV2(), new StoreTracksCommand(propeller(), new StoreUsersCommand(propeller()))),
                new LoadPlaylistTracksCommand(propeller()),
                new PlaylistStorage(propeller(), propellerRxV2(), new NewPlaylistMapper()),
                offlineContentStorage);

    }

    private OfflineContentStorage offlineContentStorageWithLikesEnabled() {
        OfflineContentStorage offlineContentStorage = new OfflineContentStorage(sharedPreferences(), Schedulers.trampoline());
        offlineContentStorage.addLikedTrackCollection().test().assertComplete();
        return offlineContentStorage;
    }

    @Test
    public void returnsLikedTracksAsPendingDownloads() throws Exception {
        ApiTrack apiTrack = insertSyncableLikedTrack(100L);

        final ExpectedOfflineContent toBeOffline = command.call(null);

        assertThat(toBeOffline.requests).containsExactly(downloadRequestFromLikes(apiTrack));
    }

    @Test
    public void returnsOfflinePlaylistTracksAsPendingDownloads() throws Exception {
        final ApiPlaylist playlist = getOfflinePlaylist();
        final ApiTrack track1 = insertSyncablePlaylistTrack(playlist, 0);

        ExpectedOfflineContent toBeOffline = command.call(null);

        assertThat(toBeOffline.requests).containsExactly(
                downloadRequestFromPlaylists(track1));
    }

    @Test
    public void returnsEmptyListWhenLikesNotMarkedForOffline() throws Exception {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(10));

        offlineContentStorage.removeLikedTrackCollection().test().assertComplete();

        ExpectedOfflineContent toBeOffline = command.call(null);

        assertThat(toBeOffline.requests).isEmpty();
    }

    @Test
    public void doesNotReturnTracksWithoutPolicies() {
        testFixtures().insertLikedTrack(new Date(10));
        propeller().delete(Tables.TrackPolicies.TABLE);

        ExpectedOfflineContent toBeOffline = command.call(null);

        assertThat(toBeOffline.requests).isEmpty();
    }

    @Test
    public void returnsLikesPendingDownloadOrderedByLikeDate() throws Exception {
        final ApiTrack apiTrack1 = insertSyncableLikedTrack(100);
        final ApiTrack apiTrack2 = insertSyncableLikedTrack(200);
        final ApiTrack apiTrack3 = insertSyncableLikedTrack(300);

        ExpectedOfflineContent toBeOffline = command.call(null);

        assertThat(toBeOffline.requests).containsExactly(
                downloadRequestFromLikes(apiTrack3),
                downloadRequestFromLikes(apiTrack2),
                downloadRequestFromLikes(apiTrack1)
        );
    }

    @Test
    public void returnsOfflinePlaylistTracksOrderedByPosition() throws Exception {
        final ApiPlaylist playlist = getOfflinePlaylist();
        final ApiTrack playlistTrack1 = insertSyncablePlaylistTrack(playlist, 1);
        final ApiTrack playlistTrack0 = insertSyncablePlaylistTrack(playlist, 0);

        ExpectedOfflineContent toBeOffline = command.call(null);

        assertThat(toBeOffline.requests).containsExactly(
                downloadRequestFromPlaylists(playlistTrack0),
                downloadRequestFromPlaylists(playlistTrack1)
        );
    }

    @Test
    public void doesNotReturnTracksNotInOfflinePlaylists() {
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        insertSyncablePlaylistTrack(playlist, 1);

        ExpectedOfflineContent toBeOffline = command.call(null);

        assertThat(toBeOffline.requests).isEmpty();
    }

    @Test
    public void returnsOfflinePlaylistsOrderedByPlaylistLikedOrThenCreationDate() throws Exception {
        final Date OLDEST = new Date(0);
        final Date OLDER = new Date(100);
        final Date OLD = new Date(200L);
        final Date NEW = new Date(300L);

        ApiUser user = testFixtures().insertUser();

        final ApiPlaylist olderCreatedPlaylist = testFixtures().insertPlaylistWithCreationDate(user, OLDER);
        markPlaylistAsOffline(olderCreatedPlaylist.getUrn());
        final ApiTrack trackInOlderCreatedPlaylist = insertSyncablePlaylistTrack(olderCreatedPlaylist, 0);

        final ApiPlaylist newlyLikedPlaylist = testFixtures().insertLikedPlaylist(OLDEST, NEW);
        markPlaylistAsOffline(newlyLikedPlaylist.getUrn());
        final ApiTrack trackInNewlyLikedPlaylist = insertSyncablePlaylistTrack(newlyLikedPlaylist, 0);

        final ApiPlaylist oldCreatedPlaylist = testFixtures().insertPlaylistWithCreationDate(user, OLD);
        markPlaylistAsOffline(oldCreatedPlaylist.getUrn());
        final ApiTrack trackInOldCreatedPlaylist = insertSyncablePlaylistTrack(oldCreatedPlaylist, 0);

        ExpectedOfflineContent toBeOffline = command.call(null);

        assertThat(toBeOffline.requests).containsExactly(
                downloadRequestFromPlaylists(trackInNewlyLikedPlaylist),
                downloadRequestFromPlaylists(trackInOldCreatedPlaylist),
                downloadRequestFromPlaylists(trackInOlderCreatedPlaylist)
        );
    }

    @Test
    public void returnsOfflinePlaylistTracksBeforeLikes() throws Exception {
        final ApiTrack apiTrack = insertSyncableLikedTrack(100);

        final ApiPlaylist playlist = getOfflinePlaylist();
        final ApiTrack playlistTrack = insertSyncablePlaylistTrack(playlist, 0);

        ExpectedOfflineContent toBeOffline = command.call(null);

        assertThat(toBeOffline.requests).containsExactly(
                downloadRequestFromPlaylists(playlistTrack),
                downloadRequestFromLikes(apiTrack)
        );
    }

    @Test
    public void doesNotReturnDuplicatedDownloadRequests() throws Exception {
        final ApiTrack apiTrack = insertSyncableLikedTrack(100);
        final ApiPlaylist playlist = getOfflinePlaylist();

        testFixtures().insertPlaylistTrack(playlist.getUrn(), apiTrack.getUrn(), 0);

        ExpectedOfflineContent toBeOffline = command.call(null);

        assertThat(toBeOffline.requests)
                .containsExactly(downloadRequestFromLikesAndPlaylists(apiTrack));
    }

    @Test
    public void doesNotReturnLikedTrackWhenPolicyUpdateHappenedAfterTheLast30Days() {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(10));
        testFixtures().insertPolicyAllow(apiTrack.getUrn(), NOW - TimeUnit.DAYS.toMillis(30));

        final ExpectedOfflineContent toBeOffline = command.call(null);

        assertThat(toBeOffline.requests).isEmpty();
    }

    @Test
    public void doesNotIncludeReturnRemovedLikes() {
        ApiTrack apiTrack = testFixtures().insertLikedTrackPendingRemoval(new Date(0), new Date(10));
        testFixtures().insertPolicyAllow(apiTrack.getUrn(), NOW);

        final ExpectedOfflineContent toBeOffline = command.call(null);

        assertThat(toBeOffline.requests).isEmpty();
    }

    @Test
    public void doesNotIncludeTracksRemovedFromAPlaylist() {
        final ApiPlaylist playlist = getOfflinePlaylist();
        final ApiTrack playlistTrack1 = testFixtures().insertPlaylistTrackPendingRemoval(playlist, 1, new Date(NOW));
        testFixtures().insertPolicyAllow(playlistTrack1.getUrn(), NOW);

        ExpectedOfflineContent toBeOffline = command.call(null);

        assertThat(toBeOffline.requests).isEmpty();
    }

    @Test
    public void doesNotReturnOfflinePlaylistTracksWhenPolicyUpdateHappenedAfterTheLast30Days() throws Exception {
        final ApiPlaylist playlist = getOfflinePlaylist();
        final ApiTrack track1 = testFixtures().insertPlaylistTrack(playlist, 0);
        Urn urn = track1.getUrn();
        testFixtures().insertPolicyAllow(urn, NOW - TimeUnit.DAYS.toMillis(30));

        ExpectedOfflineContent toBeOffline = command.call(null);

        assertThat(toBeOffline.requests).isEmpty();
    }

    @Test
    public void commandReturnsPlaylistsWithoutTracks() {
        final ApiPlaylist playlist = getOfflinePlaylist();
        testFixtures().insertPlaylistTrack(playlist, 0);
        final ApiPlaylist playlistWithoutTracks = getOfflinePlaylist();

        assertThat(command.call(null).emptyPlaylists).containsExactly(playlistWithoutTracks.getUrn());
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

    private ApiPlaylist getOfflinePlaylist() {
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        markPlaylistAsOffline(playlist.getUrn());
        return playlist;
    }

    private void markPlaylistAsOffline(Urn urn) {
        offlineContentStorage.storeAsOfflinePlaylists(Collections.singletonList(urn)).test().assertComplete();
    }

}
