package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.playlists.LocalPlaylistChange;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.collections.Sets;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class PlaylistStorageTest extends StorageIntegrationTest {

    private PlaylistStorage storage;

    @Before
    public void setUp() {
        storage = new PlaylistStorage(propeller(), propellerRx(), new NewPlaylistMapper());
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

        TestSubscriber<List<Urn>> testSubscriber = new TestSubscriber<>();
        storage.availablePlaylists(Sets.newHashSet(apiPlaylist.getUrn(), Urn.forPlaylist(9876))).subscribe(testSubscriber);

        testSubscriber.assertValue(Collections.singletonList(apiPlaylist.getUrn()));
    }

    @Test
    public void loadPlaylistEntities() {
        ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();

        TestSubscriber<List<Playlist>> testSubscriber = new TestSubscriber<>();
        storage.loadPlaylists(Sets.newHashSet(apiPlaylist.getUrn())).subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertValueCount(1);
        List<Playlist> playlistEntities = testSubscriber.getOnNextEvents().get(0);
        assertThat(playlistEntities.size()).isEqualTo(1);

        assertPlaylistsMatch(apiPlaylist, playlistEntities.get(0));
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
}
