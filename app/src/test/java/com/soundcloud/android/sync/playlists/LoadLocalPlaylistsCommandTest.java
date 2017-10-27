package com.soundcloud.android.sync.playlists;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.testsupport.PlaylistFixtures;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class LoadLocalPlaylistsCommandTest extends StorageIntegrationTest {

    private LoadLocalPlaylistsCommand command;

    @Before
    public void setUp() throws Exception {
        command = new LoadLocalPlaylistsCommand(propeller());
    }

    @Test
    public void loadsLocalPlaylists() throws Exception {
        ApiPlaylist playlist = testFixtures().insertLocalPlaylist();
        ApiPlaylist privatePlaylist = testFixtures().insertLocalPlaylist(PlaylistFixtures.apiPlaylistBuilder().sharing(Sharing.PRIVATE).build());
        testFixtures().insertPlaylist();
        testFixtures().insertTrack();

        final List<LocalPlaylistChange> call = command.call();
        assertPlaylistContainsLocalFields(call.get(0), PlaylistFixtures.playlistItem(privatePlaylist));
        assertPlaylistContainsLocalFields(call.get(1), PlaylistFixtures.playlistItem(playlist));
    }

    private void assertPlaylistContainsLocalFields(LocalPlaylistChange local, PlaylistItem expected) {
        assertThat(local.urn()).isEqualTo(expected.getUrn());
        assertThat(local.title()).isEqualTo(expected.title());
        assertThat(local.isPrivate()).isEqualTo(expected.isPrivate());
    }
}
