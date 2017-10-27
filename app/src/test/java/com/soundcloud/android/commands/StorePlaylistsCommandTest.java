package com.soundcloud.android.commands;

import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;
import static java.util.Arrays.asList;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.testsupport.PlaylistFixtures;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
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
        final List<ApiPlaylist> playlists = PlaylistFixtures.apiPlaylists(2);

        command.call(playlists);

        databaseAssertions().assertPlaylistWithUserInserted(playlists.get(0));
        databaseAssertions().assertPlaylistWithUserInserted(playlists.get(1));
    }

    @Test
    public void shouldStorePlaylistsUsingUpsert() throws Exception {
        ApiPlaylist updatedPlaylist = testFixtures().insertPlaylist().toBuilder().title("new title").build();

        command.call(asList(updatedPlaylist));

        assertThat(select(from(Tables.Sounds.TABLE))).counts(1);
        databaseAssertions().assertPlaylistInserted(updatedPlaylist);
    }
}
