package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.collections.Sets;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.TestSubscriber;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class PlaylistStorageTest extends StorageIntegrationTest {

    private static final Urn LOGGED_IN_USER = Urn.forUser(123L);

    private PlaylistStorage storage;

    @Mock AccountOperations accountOperations;

    @Before
    public void setUp() {
        storage = new PlaylistStorage(propeller(), propellerRx(), accountOperations, new NewPlaylistMapper());
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
    public void availablePlaylistsReturnsUrnsForAvailablePlaylist() throws Exception {
        ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();

        TestSubscriber<List<Urn>> testSubscriber = new TestSubscriber<>();
        storage.availablePlaylists(Sets.newHashSet(apiPlaylist.getUrn(), Urn.forPlaylist(9876))).subscribe(testSubscriber);

        testSubscriber.assertValue(Collections.singletonList(apiPlaylist.getUrn()));
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

        assertPlaylistsMatch(apiPlaylist, playlistEntities.get(0));
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
}
