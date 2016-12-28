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

        final List<PlaylistItem> call = command.call();
        assertPlaylistContainsLocalFields(call.get(0), PlaylistItem.from(privatePlaylist));
        assertPlaylistContainsLocalFields(call.get(1), PlaylistItem.from(playlist));
    }

    private void assertPlaylistContainsLocalFields(PlaylistItem local, PlaylistItem expected) {
        assertThat(local.getUrn()).isEqualTo(expected.getUrn());
        assertThat(local.getTitle()).isEqualTo(expected.getTitle());
        assertThat(local.isPrivate()).isEqualTo(expected.isPrivate());
    }
}
