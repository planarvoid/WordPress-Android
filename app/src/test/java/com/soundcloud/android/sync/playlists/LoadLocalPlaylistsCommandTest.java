package com.soundcloud.android.sync.playlists;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
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
        final ApiPlaylist privatePlaylist = ModelFixtures.create(ApiPlaylist.class);
        privatePlaylist.setSharing(Sharing.PRIVATE);
        testFixtures().insertLocalPlaylist(privatePlaylist);
        testFixtures().insertPlaylist();
        testFixtures().insertTrack();

        final List<LocalPlaylistChange> call = command.call();
        assertPlaylistContainsLocalFields(call.get(0), ModelFixtures.playlistItem(privatePlaylist));
        assertPlaylistContainsLocalFields(call.get(1), ModelFixtures.playlistItem(playlist));
    }

    private void assertPlaylistContainsLocalFields(LocalPlaylistChange local, PlaylistItem expected) {
        assertThat(local.urn()).isEqualTo(expected.getUrn());
        assertThat(local.title()).isEqualTo(expected.title());
        assertThat(local.isPrivate()).isEqualTo(expected.isPrivate());
    }
}
