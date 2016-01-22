package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

public class LoadOfflinePlaylistsCommandTest extends StorageIntegrationTest {

    private LoadOfflinePlaylistsCommand command;

    @Before
    public void setUp() throws Exception {
        command = new LoadOfflinePlaylistsCommand(propeller());
    }

    @Test
    public void loadsOfflinePlaylists() {
        final ApiPlaylist offlinePlaylist = testFixtures().insertPlaylistMarkedForOfflineSync();
        testFixtures().insertPlaylist();

        assertThat(command.call(null)).containsExactly(offlinePlaylist.getUrn());
    }
}
