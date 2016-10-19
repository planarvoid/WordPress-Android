package com.soundcloud.android.commands;

import static com.soundcloud.android.storage.Table.Sounds;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;
import static java.util.Arrays.asList;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class StorePlaylistsCommandTest extends StorageIntegrationTest {

    private StorePlaylistsCommand command;

    @Before
    public void setup() {
        command = new StorePlaylistsCommand(propeller(), new StoreUsersCommand(propeller()));
    }

    @Test
    public void shouldPersistPlaylistsWithCreatorsInDatabase() throws Exception {
        final List<ApiPlaylist> playlists = ModelFixtures.create(ApiPlaylist.class, 2);

        command.call(playlists);

        databaseAssertions().assertPlaylistWithUserInserted(playlists.get(0));
        databaseAssertions().assertPlaylistWithUserInserted(playlists.get(1));
    }

    @Test
    public void shouldStorePlaylistsUsingUpsert() throws Exception {
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        playlist.setTitle("new title");

        command.call(asList(playlist));

        assertThat(select(from(Sounds.name()))).counts(1);
        databaseAssertions().assertPlaylistInserted(playlist);
    }
}
