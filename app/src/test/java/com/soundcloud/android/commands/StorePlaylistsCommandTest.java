package com.soundcloud.android.commands;

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
public class StorePlaylistsCommandTest extends StorageIntegrationTest {

    private StorePlaylistsCommand command;

    @Before
    public void setup() {
        command = new StorePlaylistsCommand(propeller());
    }

    @Test
    public void shouldPersistPlaylistsWithCreatorsInDatabase() throws Exception {
        final List<ApiPlaylist> playlists = ModelFixtures.create(ApiPlaylist.class, 2);

        command.with(playlists).call();

        databaseAssertions().assertPlaylistWithUserInserted(playlists.get(0));
        databaseAssertions().assertPlaylistWithUserInserted(playlists.get(1));
    }

    @Test
    public void shouldStorePlaylistsUsingUpsert() throws Exception {
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        playlist.setTitle("new title");

        command.with(Arrays.asList(playlist)).call();

        assertThat(select(from(Table.Sounds.name())), counts(1));
        databaseAssertions().assertPlaylistInserted(playlist);
    }
}