package com.soundcloud.android.storage;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistStorageTest extends StorageIntegrationTest {

    private PlaylistStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new PlaylistStorage(propeller());
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
}