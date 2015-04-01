package com.soundcloud.android.storage;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistStorageTest extends StorageIntegrationTest {

    private PlaylistStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new PlaylistStorage(propeller(), propellerRx());
    }

    @Test
    public void hasLocalPlaylistsIsFalseWithNoPlaylists() throws Exception {
        testFixtures().insertPlaylist();

        expect(storage.hasLocalPlaylists()).toBeFalse();
    }

    @Test
    public void hasLocalPlaylistsIsTrueWithLocalPlaylist() throws Exception {
        testFixtures().insertLocalPlaylist();

        expect(storage.hasLocalPlaylists()).toBeTrue();
    }

    @Test
    public void hasPlaylistDueForSyncReturnsOnlyPlaylistWithUnpushedTracks() throws Exception {
        testFixtures().insertPlaylist();
        ApiPlaylist playlistWithAddition = testFixtures().insertPlaylist();
        ApiPlaylist playlistWithRemoval = testFixtures().insertPlaylist();
        testFixtures().insertPlaylistTrackPendingAddition(playlistWithAddition, 0, new Date());
        testFixtures().insertPlaylistTrackPendingRemoval(playlistWithRemoval, 1, new Date());

        expect(storage.getPlaylistsDueForSync()).toContainExactly(playlistWithAddition.getUrn(), playlistWithRemoval.getUrn());
    }

    @Test
    public void loadPlaylistReturnsEmptyPropertySetIfNotStored() throws Exception {
        PropertySet playlist = storage.loadPlaylist(Urn.forPlaylist(123)).toBlocking().single();

        expect(playlist).toEqual(PropertySet.create());
    }

    @Test
    public void loadsPlaylistFromDatabase() throws Exception {
        ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();

        PropertySet playlist = storage.loadPlaylist(apiPlaylist.getUrn()).toBlocking().single();

        expect(playlist).toEqual(TestPropertySets.fromApiPlaylist(apiPlaylist, false, false, false));
    }

    @Test
    public void loadsPlaylistWithTrackCountAsMaximumOfLocalAndRemoteFromDatabase() throws Exception {
        ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();

        expect(apiPlaylist.getTrackCount()).toEqual(2);

        final Urn playlistUrn = apiPlaylist.getUrn();
        testFixtures().insertPlaylistTrack(playlistUrn, 0);
        testFixtures().insertPlaylistTrack(playlistUrn, 1);
        testFixtures().insertPlaylistTrack(playlistUrn, 2);

        PropertySet playlist = storage.loadPlaylist(apiPlaylist.getUrn()).toBlocking().single();

        expect(playlist.get(PlaylistProperty.URN)).toEqual(playlistUrn);
        expect(playlist.get(PlaylistProperty.TRACK_COUNT)).toEqual(3);
    }

    @Test
    public void loadsLikedPlaylistFromDatabase() throws Exception {
        ApiPlaylist apiPlaylist = testFixtures().insertLikedPlaylist(new Date(100));

        PropertySet playlist = storage.loadPlaylist(apiPlaylist.getUrn()).toBlocking().single();

        expect(playlist).toEqual(TestPropertySets.fromApiPlaylist(apiPlaylist, true, false, false));
    }

    @Test
    public void loadsRepostedPlaylistFromDatabase() throws Exception {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        testFixtures().insertPlaylistRepost(apiPlaylist.getId(), 123L);

        PropertySet playlist = storage.loadPlaylist(apiPlaylist.getUrn()).toBlocking().single();

        expect(playlist).toEqual(TestPropertySets.fromApiPlaylist(apiPlaylist, false, true, false));
    }

    @Test
    public void loadsMarkedForOfflineAvailabilityPlaylistFromDatabase() throws Exception {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylistMarkedForOfflineSync();

        PropertySet playlist = storage.loadPlaylist(apiPlaylist.getUrn()).toBlocking().single();

        expect(playlist).toEqual(TestPropertySets.fromApiPlaylist(apiPlaylist, false, false, true));
    }
}