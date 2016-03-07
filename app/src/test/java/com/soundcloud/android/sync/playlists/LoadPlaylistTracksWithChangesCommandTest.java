package com.soundcloud.android.sync.playlists;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.playlists.PlaylistTrackProperty;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.collections.PropertySet;
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

        List<PropertySet> playlistTracks = command.with(playlist.getUrn()).call();

        assertThat(playlistTracks).containsExactly(
                PropertySet.from(
                        PlaylistTrackProperty.TRACK_URN.bind(track1.getUrn())
                ),
                PropertySet.from(
                        PlaylistTrackProperty.TRACK_URN.bind(track2.getUrn()),
                        PlaylistTrackProperty.ADDED_AT.bind(new Date(200))
                ),
                PropertySet.from(
                        PlaylistTrackProperty.TRACK_URN.bind(track3.getUrn()),
                        PlaylistTrackProperty.REMOVED_AT.bind(new Date(100))
                )
        );
    }
}
