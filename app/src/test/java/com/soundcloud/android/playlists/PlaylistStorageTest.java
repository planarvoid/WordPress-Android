package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.collections.Sets;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.TestSubscriber;

import java.util.Date;
import java.util.List;

public class PlaylistStorageTest extends StorageIntegrationTest {

    private static final Urn LOGGED_IN_USER = Urn.forUser(123L);

    private PlaylistStorage storage;

    @Mock AccountOperations accountOperations;

    @Before
    public void setUp() {
        storage = new PlaylistStorage(propeller(), propellerRx(), accountOperations);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(LOGGED_IN_USER);
    }

    @Test
    public void hasLocalChangesIsFalseWithNoPlaylists() {
        testFixtures().insertPlaylist();

        assertThat(storage.hasLocalChanges()).isFalse();
    }

    @Test
    public void hasLocalChangesIsTrueWithLocalPlaylist() {
        testFixtures().insertLocalPlaylist();

        assertThat(storage.hasLocalChanges()).isTrue();
    }

    @Test
    public void hasLocalChangesIsTrueWhenPlaylistMarkedForRemoval() {
        testFixtures().insertPlaylistPendingRemoval();

        assertThat(storage.hasLocalChanges()).isTrue();
    }

    @Test
    public void hasPlaylistDueForSyncReturnsOnlyRemotePlaylistWithUnpushedTracks() {
        testFixtures().insertPlaylist();
        final ApiPlaylist localPlaylist = testFixtures().insertLocalPlaylist();
        ApiPlaylist playlistWithAddition = testFixtures().insertPlaylist();
        ApiPlaylist playlistWithRemoval = testFixtures().insertPlaylist();

        testFixtures().insertPlaylistTrackPendingAddition(localPlaylist, 0, new Date());
        testFixtures().insertPlaylistTrackPendingAddition(playlistWithAddition, 0, new Date());
        testFixtures().insertPlaylistTrackPendingRemoval(playlistWithRemoval, 1, new Date());

        assertThat(storage.getPlaylistsDueForSync()).contains(playlistWithAddition.getUrn(),
                                                              playlistWithRemoval.getUrn());
    }

    @Test
    public void loadPlaylistEntities() throws Exception {
        ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();

        TestSubscriber<List<PlaylistItem>> testSubscriber = new TestSubscriber<>();
        storage.loadPlaylists(Sets.newHashSet(apiPlaylist.getUrn())).subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertValueCount(1);
        List<PlaylistItem> playlistEntities = testSubscriber.getOnNextEvents().get(0);
        assertThat(playlistEntities.size()).isEqualTo(1);

        PlaylistItem entity = playlistEntities.get(0);
        assertPlaylistsMatch(apiPlaylist, entity);
    }

    private void assertPlaylistsMatch(ApiPlaylist apiPlaylist, PlaylistItem entity) {
        assertThat(entity.getUrn()).isEqualTo(apiPlaylist.getUrn());
        assertThat(entity.getTitle()).isEqualTo(apiPlaylist.getTitle());
        assertThat(entity.getCreatorName()).isEqualTo(apiPlaylist.getUsername());
        assertThat(entity.getUserUrn()).isEqualTo(apiPlaylist.getUser().getUrn());
        assertThat(entity.getDuration()).isEqualTo(apiPlaylist.getDuration());
        assertThat(entity.getTrackCount()).isEqualTo(apiPlaylist.getTrackCount());
        assertThat(entity.getLikesCount()).isEqualTo(apiPlaylist.getLikesCount());
        assertThat(entity.getRepostCount()).isEqualTo(apiPlaylist.getRepostsCount());
        assertThat(entity.getCreatedAt()).isEqualTo(apiPlaylist.getCreatedAt());
        assertThat(entity.getImageUrlTemplate()).isEqualTo(apiPlaylist.getImageUrlTemplate());
        assertThat(entity.isAlbum()).isEqualTo(apiPlaylist.isAlbum());
        assertThat(entity.isMarkedForOffline()).isEqualTo(Optional.of(false));
    }

    @Test
    public void loadPlaylistReturnsNothingIfNotStored() {
        TestSubscriber<PropertySet> subscriber = new TestSubscriber<>();
        storage.loadPlaylist(Urn.forPlaylist(123)).subscribe(subscriber);

        subscriber.assertNoValues();
        subscriber.assertCompleted();
    }

    @Test
    public void loadsPlaylistFromDatabase() {
        ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();

        PropertySet playlist = storage.loadPlaylist(apiPlaylist.getUrn()).toBlocking().single();

        assertThat(playlist).isEqualTo(TestPropertySets.fromApiPlaylist(apiPlaylist, false, false, false, false));
    }

    @Test
    public void loadsPlaylistWithTrackCountAsMaximumOfLocalAndRemoteFromDatabase() {
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
    public void loadsLikedPlaylistFromDatabase() {
        ApiPlaylist apiPlaylist = testFixtures().insertLikedPlaylist(new Date(100));

        PropertySet playlist = storage.loadPlaylist(apiPlaylist.getUrn()).toBlocking().single();

        assertThat(playlist).isEqualTo(TestPropertySets.fromApiPlaylist(apiPlaylist, true, false, false, false));
    }

    @Test
    public void loadsRepostedPlaylistFromDatabase() {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        testFixtures().insertPlaylistRepost(apiPlaylist.getId(), 123L);

        PropertySet playlist = storage.loadPlaylist(apiPlaylist.getUrn()).toBlocking().single();

        assertThat(playlist).isEqualTo(TestPropertySets.fromApiPlaylist(apiPlaylist, false, true, false, false));
    }

    @Test
    public void loadsMarkedForOfflineAvailabilityPlaylistFromDatabase() {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylistMarkedForOfflineSync();

        PropertySet playlist = storage.loadPlaylist(apiPlaylist.getUrn()).toBlocking().single();

        assertThat(playlist.slice(
                EntityProperty.URN,
                PlayableProperty.TITLE,
                PlaylistProperty.PLAYLIST_DURATION,
                PlayableProperty.CREATOR_NAME,
                PlayableProperty.CREATOR_URN,
                PlayableProperty.LIKES_COUNT,
                PlayableProperty.REPOSTS_COUNT,
                PlayableProperty.PERMALINK_URL,
                EntityProperty.IMAGE_URL_TEMPLATE,
                PlayableProperty.CREATED_AT,
                PlayableProperty.IS_PRIVATE,
                PlayableProperty.IS_USER_LIKE,
                PlayableProperty.IS_USER_REPOST,
                PlaylistProperty.IS_POSTED,
                OfflineProperty.IS_MARKED_FOR_OFFLINE,
                PlaylistProperty.TRACK_COUNT,
                PlaylistProperty.IS_ALBUM,
                PlaylistProperty.SET_TYPE,
                PlaylistProperty.RELEASE_DATE
                   )
        ).isEqualTo(
                TestPropertySets.fromApiPlaylist(apiPlaylist, false, false, true, false)
        );
    }

    @Test
    public void loadRequestedDownloadStateWhenPlaylistIsMarkedForOfflineAndHasDownloadRequests() {
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
    public void loadDownloadedStateWhenPlaylistIsMarkedForOfflineAndNoDownloadRequest() {
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
    public void loadDownloadedStateWhenPlaylistHasOnlyCreatorOptedOutTracks() {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack track = testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        testFixtures().insertUnavailableTrackDownload(track.getUrn(), 123L);

        PropertySet playlist = storage.loadPlaylist(apiPlaylist.getUrn()).toBlocking().single();

        final PropertySet expected = TestPropertySets
                .fromApiPlaylist(apiPlaylist, false, false, true, false)
                .put(OfflineProperty.OFFLINE_STATE, OfflineState.UNAVAILABLE);

        assertThat(playlist).isEqualTo(expected);
    }

    @Test
    public void loadDownloadedStateWhenPlaylistHasSomeCreatorOptedOutTracks() {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack track1 = testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        testFixtures().insertCompletedTrackDownload(track1.getUrn(), 123L, System.currentTimeMillis());
        final ApiTrack track2 = testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        testFixtures().insertUnavailableTrackDownload(track2.getUrn(), 123L);

        PropertySet playlist = storage.loadPlaylist(apiPlaylist.getUrn()).toBlocking().single();

        final PropertySet expected = TestPropertySets
                .fromApiPlaylist(apiPlaylist, false, false, true, false)
                .put(OfflineProperty.OFFLINE_STATE, OfflineState.DOWNLOADED);

        assertThat(playlist).isEqualTo(expected);
    }

    @Test
    public void loadDownloadedStateOfTwoDifferentPlaylistsDoesNotInfluenceEachOther() {
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
    public void loadsPostedPlaylistFromDatabase() {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        when(accountOperations.getLoggedInUserUrn()).thenReturn(apiPlaylist.getUser().getUrn());

        PropertySet playlist = storage.loadPlaylist(apiPlaylist.getUrn()).toBlocking().single();

        assertThat(playlist).isEqualTo(TestPropertySets.fromApiPlaylist(apiPlaylist, false, false, false, true));
    }

    @Test
    public void loadPlaylistModificationsReturnsEmptySetWhenNoModifications() {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        when(accountOperations.getLoggedInUserUrn()).thenReturn(apiPlaylist.getUser().getUrn());

        assertThat(storage.loadPlaylistModifications(apiPlaylist.getUrn())).isEmpty();
    }

    @Test
    public void loadPlaylistModificationsLoadsNewInfoWithModifications() {
        final ApiPlaylist apiPlaylist = testFixtures().insertModifiedPlaylist(new Date());
        when(accountOperations.getLoggedInUserUrn()).thenReturn(apiPlaylist.getUser().getUrn());

        PropertySet playlist = storage.loadPlaylistModifications(apiPlaylist.getUrn());

        assertThat(playlist).isEqualTo(PropertySet.from(
                TrackProperty.URN.bind(apiPlaylist.getUrn()),
                PlayableProperty.TITLE.bind(apiPlaylist.getTitle()),
                PlayableProperty.IS_PRIVATE.bind(Sharing.PRIVATE.equals(apiPlaylist.getSharing()))));
    }

    @Test
    public void loadsPostedPlaylistWithSetTypeAndReleaseDate() {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylistAlbum("ep", "2010-10-10");
        when(accountOperations.getLoggedInUserUrn()).thenReturn(apiPlaylist.getUser().getUrn());

        PropertySet playlist = storage.loadPlaylist(apiPlaylist.getUrn()).toBlocking().single();

        assertThat(playlist.get(PlaylistProperty.SET_TYPE)).isEqualTo("ep");
        assertThat(playlist.get(PlaylistProperty.RELEASE_DATE)).isEqualTo("2010-10-10");
    }

    @Test
    public void loadsPostedPlaylistWithoutSetTypeAndReleaseDateWhenNull() {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylistAlbum(null, null);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(apiPlaylist.getUser().getUrn());

        PropertySet playlist = storage.loadPlaylist(apiPlaylist.getUrn()).toBlocking().single();

        assertThat(playlist.contains(PlaylistProperty.SET_TYPE)).isEqualTo(false);
        assertThat(playlist.contains(PlaylistProperty.RELEASE_DATE)).isEqualTo(false);
    }
}
