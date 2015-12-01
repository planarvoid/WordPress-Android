package com.soundcloud.android.sync.playlists;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

public class RemoveLocalPlaylistsCommandTest extends StorageIntegrationTest {

    private RemoveLocalPlaylistsCommand command;

    @Before
    public void setUp() throws Exception {
        command = new RemoveLocalPlaylistsCommand(propeller());
    }

    @Test
    public void removesLocalPlayists() throws Exception {
        ApiPlaylist remotePlaylist = testFixtures().insertPlaylist();
        ApiPlaylist localPlaylist = testFixtures().insertLocalPlaylist();

        command.call(null);

        databaseAssertions().assertPlaylistInserted(remotePlaylist.getId());
        databaseAssertions().assertPlaylistNotStored(localPlaylist);
    }
}
