package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

public class LoadPlaylistPendingRemovalCommandTest extends StorageIntegrationTest {

    private LoadPlaylistPendingRemovalCommand command;

    @Before
    public void setUp() throws Exception {
        command = new LoadPlaylistPendingRemovalCommand(propeller());
    }

    @Test
    public void returnsEmptyWhenNoPendingRemovalPlaylist() {
        testFixtures().insertPlaylist();

        assertThat(command.call(null)).isEmpty();
    }

    @Test
    public void returnsPendingRemovalPlaylists() {
        testFixtures().insertPlaylist();
        final ApiPlaylist removedPlaylist1 = testFixtures().insertPlaylistPendingRemoval();
        final ApiPlaylist removedPlaylist2 = testFixtures().insertPlaylistPendingRemoval();

        assertThat(command.call(null)).containsExactly(removedPlaylist1.getUrn(), removedPlaylist2.getUrn());
    }

}
