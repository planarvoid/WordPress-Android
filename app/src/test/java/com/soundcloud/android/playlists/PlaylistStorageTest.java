package com.soundcloud.android.playlists;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.playlists.LocalPlaylistChange;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.collections.Sets;
import com.soundcloud.java.optional.Optional;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class PlaylistStorageTest extends StorageIntegrationTest {

    private PlaylistStorage storage;

    @Before
    public void setUp() {
        storage = new PlaylistStorage(propeller(), propellerRxV2(), new NewPlaylistMapper());
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
    public void hasLocalChangesIsTrueWithLocalTrackAddition() {
        testFixtures().insertPlaylistTrackPendingAddition(testFixtures().insertPlaylist(), 0, new Date());

        assertThat(storage.hasLocalChanges()).isTrue();
    }

    @Test
    public void hasLocalChangesIsTrueWithLocalTrackRemoval() {
        testFixtures().insertPlaylistTrackPendingRemoval(testFixtures().insertPlaylist(), 0, new Date());

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

        assertThat(storage.playlistWithTrackChanges()).contains(playlistWithAddition.getUrn(), playlistWithRemoval.getUrn());
    }

    @Test
    public void availablePlaylistsReturnsUrnsForAvailablePlaylist() {
        ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();

        final TestObserver<List<Urn>> testSubscriber = storage.availablePlaylists(Sets.newHashSet(apiPlaylist.getUrn(), Urn.forPlaylist(9876))).test();

        testSubscriber.assertValue(Collections.singletonList(apiPlaylist.getUrn()));
    }

    @Test
    public void loadPlaylistEntities() {
        ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();

        final TestObserver<List<Playlist>> testSubscriber = storage.loadPlaylists(Sets.newHashSet(apiPlaylist.getUrn())).test();

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertValueCount(1);
        List<Playlist> playlistEntities = testSubscriber.values().get(0);
        assertThat(playlistEntities.size()).isEqualTo(1);

        assertPlaylistsMatch(apiPlaylist, playlistEntities.get(0));
    }

    @Test
    public void shouldReturnTrackCountAsMaximumOfRemoteAndLocalCounts() throws Exception {
        ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();

        assertThat(apiPlaylist.getTrackCount()).isEqualTo(2);

        final Urn playlistUrn = apiPlaylist.getUrn();
        testFixtures().insertPlaylistTrack(playlistUrn, 0);
        testFixtures().insertPlaylistTrack(playlistUrn, 1);
        testFixtures().insertPlaylistTrack(playlistUrn, 2);

        final TestObserver<List<Playlist>> testSubscriber = storage.loadPlaylists(Sets.newHashSet(apiPlaylist.getUrn())).test();

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertValueCount(1);
        List<Playlist> playlistEntities = testSubscriber.values().get(0);
        assertThat(playlistEntities.size()).isEqualTo(1);

        assertThat(playlistEntities.get(0).trackCount()).isEqualTo(3);
    }

    private void assertPlaylistsMatch(ApiPlaylist apiPlaylist, Playlist entity) {
        assertThat(entity.urn()).isEqualTo(apiPlaylist.getUrn());
        assertThat(entity.title()).isEqualTo(apiPlaylist.getTitle());
        assertThat(entity.creatorName()).isEqualTo(apiPlaylist.getUsername());
        assertThat(entity.creatorUrn()).isEqualTo(apiPlaylist.getUser().getUrn());
        assertThat(entity.duration()).isEqualTo(apiPlaylist.getDuration());
        assertThat(entity.trackCount()).isEqualTo(apiPlaylist.getTrackCount());
        assertThat(entity.likesCount()).isEqualTo(apiPlaylist.getLikesCount());
        assertThat(entity.repostCount()).isEqualTo(apiPlaylist.getRepostsCount());
        assertThat(entity.createdAt()).isEqualTo(apiPlaylist.getCreatedAt());
        assertThat(entity.imageUrlTemplate()).isEqualTo(apiPlaylist.getImageUrlTemplate());
        assertThat(entity.isAlbum()).isEqualTo(apiPlaylist.isAlbum());
        assertThat(entity.isMarkedForOffline()).isEqualTo(Optional.of(false));
        assertThat(entity.permalinkUrl().get()).isEqualTo(apiPlaylist.getPermalinkUrl());
    }

    @Test
    public void loadPlaylistModificationsReturnsEmptySetWhenNoModifications() {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();

        assertThat(storage.loadPlaylistModifications(apiPlaylist.getUrn()).isPresent()).isFalse();
    }

    @Test
    public void loadPlaylistModificationsLoadsNewInfoWithModifications() {
        final ApiPlaylist apiPlaylist = testFixtures().insertModifiedPlaylist(new Date());

        Optional<LocalPlaylistChange> playlist = storage.loadPlaylistModifications(apiPlaylist.getUrn());

        assertThat(playlist.get()).isEqualTo(LocalPlaylistChange.create(apiPlaylist.getUrn(), apiPlaylist.getTitle(), Sharing.PRIVATE.equals(apiPlaylist.getSharing())));
    }

    @Test
    public void loadsUrnByPermalink() throws Exception {
        testFixtures().insertPlaylist();
        ApiPlaylist playlist = testFixtures().insertPlaylist();
        String permalinkUrl = playlist.getPermalinkUrl();
        String permalink = permalinkUrl.replace("https://soundcloud.com/", "");

        final Urn urn = storage.urnForPermalink(permalink).blockingGet();

        assertThat(urn).isEqualTo(playlist.getUrn());
    }

    @Test
    public void loadsUrnByPermalinkNotFound() throws Exception {
        testFixtures().insertPlaylist();

        storage.urnForPermalink("testing")
               .test()
               .assertNoValues()
               .assertComplete();
    }

    @Test
    public void loadsPlaylistsWithTrack() throws Exception {
        ApiPlaylist apiPlaylist1 = testFixtures().insertPlaylist();
        ApiPlaylist apiPlaylist2 = testFixtures().insertPlaylist();
        ApiPlaylist apiPlaylist3 = testFixtures().insertPlaylist();

        ApiTrack apiTrack1 = testFixtures().insertPlaylistTrack(apiPlaylist1, 0);
        ApiTrack apiTrack2 = testFixtures().insertPlaylistTrack(apiPlaylist3, 0);

        TestObserver<Set<Urn>> playlists = storage.loadPlaylistsWithTracks(Sets.newHashSet(apiTrack1.getUrn(), apiTrack2.getUrn())).test();

        playlists.assertValue(Sets.newHashSet(apiPlaylist1.getUrn(), apiPlaylist3.getUrn())).assertComplete();

    }
}
