package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Date;

public class PlaylistStorageTest extends StorageIntegrationTest {

    private static final Urn LOGGED_IN_USER = Urn.forUser(123L);
    
    private com.soundcloud.android.playlists.PlaylistStorage storage;
    @Mock AccountOperations accountOperations;

    @Before
    public void setUp() throws Exception {
        storage = new com.soundcloud.android.playlists.PlaylistStorage(propeller(), propellerRx(), accountOperations);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(LOGGED_IN_USER);
    }

    @Test
    public void hasLocalPlaylistsIsFalseWithNoPlaylists() throws Exception {
        testFixtures().insertPlaylist();

        assertThat(storage.hasLocalPlaylists()).isFalse();
    }

    @Test
    public void hasLocalPlaylistsIsTrueWithLocalPlaylist() throws Exception {
        testFixtures().insertLocalPlaylist();

        assertThat(storage.hasLocalPlaylists()).isTrue();
    }

    @Test
    public void hasPlaylistDueForSyncReturnsOnlyRemotePlaylistWithUnpushedTracks() throws Exception {
        testFixtures().insertPlaylist();
        final ApiPlaylist localPlaylist = testFixtures().insertLocalPlaylist();
        ApiPlaylist playlistWithAddition = testFixtures().insertPlaylist();
        ApiPlaylist playlistWithRemoval = testFixtures().insertPlaylist();
        testFixtures().insertPlaylistTrackPendingAddition(localPlaylist, 0, new Date());
        testFixtures().insertPlaylistTrackPendingAddition(playlistWithAddition, 0, new Date());
        testFixtures().insertPlaylistTrackPendingRemoval(playlistWithRemoval, 1, new Date());

        assertThat(storage.getPlaylistsDueForSync()).contains(playlistWithAddition.getUrn(), playlistWithRemoval.getUrn());
    }

    @Test
    public void loadPlaylistReturnsEmptyPropertySetIfNotStored() throws Exception {
        PropertySet playlist = storage.loadPlaylist(Urn.forPlaylist(123)).toBlocking().single();

        assertThat(playlist).isEqualTo(PropertySet.create());
    }

    @Test
    public void loadsPlaylistFromDatabase() throws Exception {
        ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();

        PropertySet playlist = storage.loadPlaylist(apiPlaylist.getUrn()).toBlocking().single();

        assertThat(playlist).isEqualTo(TestPropertySets.fromApiPlaylist(apiPlaylist, false, false, false, false));
    }

    @Test
    public void loadsPlaylistWithTrackCountAsMaximumOfLocalAndRemoteFromDatabase() throws Exception {
        ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();

        assertThat(apiPlaylist.getTrackCount()).isEqualTo(2);

        final Urn playlistUrn = apiPlaylist.getUrn();
        testFixtures().insertPlaylistTrack(playlistUrn, 0);
        testFixtures().insertPlaylistTrack(playlistUrn, 1);
        testFixtures().insertPlaylistTrack(playlistUrn, 2);

        PropertySet playlist = storage.loadPlaylist(apiPlaylist.getUrn()).toBlocking().single();

        assertThat(playlist.get(PlaylistProperty.URN)).isEqualTo(playlistUrn);
        assertThat(playlist.get(PlaylistProperty.TRACK_COUNT)).isEqualTo(3);
    }

    @Test
    public void loadsLikedPlaylistFromDatabase() throws Exception {
        ApiPlaylist apiPlaylist = testFixtures().insertLikedPlaylist(new Date(100));

        PropertySet playlist = storage.loadPlaylist(apiPlaylist.getUrn()).toBlocking().single();

        assertThat(playlist).isEqualTo(TestPropertySets.fromApiPlaylist(apiPlaylist, true, false, false, false));
    }

    @Test
    public void loadsRepostedPlaylistFromDatabase() throws Exception {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        testFixtures().insertPlaylistRepost(apiPlaylist.getId(), 123L);

        PropertySet playlist = storage.loadPlaylist(apiPlaylist.getUrn()).toBlocking().single();

        assertThat(playlist).isEqualTo(TestPropertySets.fromApiPlaylist(apiPlaylist, false, true, false, false));
    }

    @Test
    public void loadsMarkedForOfflineAvailabilityPlaylistFromDatabase() throws Exception {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylistMarkedForOfflineSync();

        PropertySet playlist = storage.loadPlaylist(apiPlaylist.getUrn()).toBlocking().single();

        assertThat(playlist.slice(
                        EntityProperty.URN,
                        PlayableProperty.TITLE,
                        PlayableProperty.DURATION,
                        PlayableProperty.CREATOR_NAME,
                        PlayableProperty.CREATOR_URN,
                        PlayableProperty.LIKES_COUNT,
                        PlayableProperty.REPOSTS_COUNT,
                        PlayableProperty.PERMALINK_URL,
                        PlayableProperty.CREATED_AT,
                        PlayableProperty.IS_PRIVATE,
                        PlayableProperty.IS_LIKED,
                        PlayableProperty.IS_REPOSTED,
                        PlaylistProperty.IS_POSTED,
                        OfflineProperty.Collection.IS_MARKED_FOR_OFFLINE,
                        PlaylistProperty.TRACK_COUNT
                )
        ).isEqualTo(
                TestPropertySets.fromApiPlaylist(apiPlaylist, false, false, true, false)
        );
    }

    @Test
    public void loadRequestedDownloadStateWhenPlaylistIsMarkedForOfflineAndHasDownloadRequests() throws Exception {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack track = testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        testFixtures().insertTrackPendingDownload(track.getUrn(), System.currentTimeMillis());

        PropertySet playlist = storage.loadPlaylist(apiPlaylist.getUrn()).toBlocking().single();

        final PropertySet expected = TestPropertySets
                .fromApiPlaylist(apiPlaylist, false, false, true, false)
                .put(OfflineProperty.OFFLINE_STATE, OfflineState.REQUESTED);

        assertThat(playlist).isEqualTo(expected);
    }

    @Test
    public void loadDownloadedStateWhenPlaylistIsMarkedForOfflineAndNoDownloadRequest() throws Exception {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack track = testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        testFixtures().insertCompletedTrackDownload(track.getUrn(), 123L, System.currentTimeMillis());

        PropertySet playlist = storage.loadPlaylist(apiPlaylist.getUrn()).toBlocking().single();

        final PropertySet expected = TestPropertySets
                .fromApiPlaylist(apiPlaylist, false, false, true, false)
                .put(OfflineProperty.OFFLINE_STATE, OfflineState.DOWNLOADED);

        assertThat(playlist).isEqualTo(expected);
    }

    @Test
    public void loadDownloadedStateOfTwoDifferentPlaylistsDoesNotInfluenceEachOther() throws Exception {
        final ApiPlaylist downloadedPlaylist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack downloadedTrack = testFixtures().insertPlaylistTrack(downloadedPlaylist, 0);
        testFixtures().insertCompletedTrackDownload(downloadedTrack.getUrn(), 123L, System.currentTimeMillis());

        final ApiPlaylist requestedPlaylist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack requestedTrack = testFixtures().insertPlaylistTrack(requestedPlaylist, 0);
        testFixtures().insertTrackPendingDownload(requestedTrack.getUrn(), 123L);

        PropertySet result = storage.loadPlaylist(downloadedPlaylist.getUrn()).toBlocking().single();

        final PropertySet expected = TestPropertySets
                .fromApiPlaylist(downloadedPlaylist, false, false, true, false)
                .put(OfflineProperty.OFFLINE_STATE, OfflineState.DOWNLOADED);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void loadsPostedPlaylistFromDatabase() throws Exception {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        when(accountOperations.getLoggedInUserUrn()).thenReturn(apiPlaylist.getUser().getUrn());

        PropertySet playlist = storage.loadPlaylist(apiPlaylist.getUrn()).toBlocking().single();

        assertThat(playlist).isEqualTo(TestPropertySets.fromApiPlaylist(apiPlaylist, false, false, false, true));
    }
}