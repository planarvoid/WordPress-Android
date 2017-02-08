package com.soundcloud.android.sync.playlists;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class LoadPlaylistTracksWithChangesCommandTest extends StorageIntegrationTest {

    private LoadPlaylistTracksWithChangesCommand command;

    @Before
    public void setup() {
        command = new LoadPlaylistTracksWithChangesCommand(propeller());
    }

    @Test
    public void loadsPlaylistTracksWithAdditionAndRemovalStamps() throws Exception {
        ApiPlaylist playlist = testFixtures().insertPlaylist();
        ApiTrack track1 = testFixtures().insertPlaylistTrack(playlist, 0);
        ApiTrack track2 = testFixtures().insertPlaylistTrackPendingAddition(playlist, 1, new Date(200));
        ApiTrack track3 = testFixtures().insertPlaylistTrackPendingRemoval(playlist, 2, new Date(100));

        List<PlaylistTrackChange> playlistTrackChanges = command.with(playlist.getUrn()).call();

        assertThat(playlistTrackChanges).containsExactly(
                PlaylistTrackChange.createEmpty(track1.getUrn()),
                PlaylistTrackChange.createAdded(track2.getUrn()),
                PlaylistTrackChange.createRemoved(track3.getUrn())
        );
    }
}
