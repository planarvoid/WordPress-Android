package com.soundcloud.android.playlists;

import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistWriteStorageTest extends StorageIntegrationTest {

    private PlaylistWriteStorage storage;
    private List<ApiPlaylist> playlists;

    @Before
    public void setup() {
        storage = new PlaylistWriteStorage(propeller());
        playlists = ModelFixtures.create(ApiPlaylist.class, 2);
    }

    @Test
    public void shouldStoreApiMobilePlaylistCollection() {
        storage.storePlaylists(playlists);
        databaseAssertions().assertPlaylistsInserted(playlists);
    }

    @Test
    public void shouldStorePlaylistsUsingUpsert() {
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        playlist.setTitle("new title");

        storage.storePlaylists(Arrays.asList(playlist));

        assertThat(select(from(Table.SOUNDS.name)), counts(1));
        databaseAssertions().assertPlaylistInserted(playlist);
    }

    @Test
    public void shouldStoreUsersFromApiMobilePlaylistCollection() {
        storage.storePlaylists(playlists);

        databaseAssertions().assertPlayableUserInserted(playlists.get(0).getUser());
        databaseAssertions().assertPlayableUserInserted(playlists.get(1).getUser());
    }
}