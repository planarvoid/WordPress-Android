package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class LoadPlaylistTrackUrnsCommandTest extends StorageIntegrationTest {

    private LoadPlaylistTrackUrnsCommand command;

    @Before
    public void setup() {
        command = new LoadPlaylistTrackUrnsCommand(propeller());
    }

    @Test
    public void shouldLoadPlaylistTrackUrns() throws Exception {
        ApiPlaylist playlist = testFixtures().insertPlaylist();
        ApiTrack track1 = testFixtures().insertPlaylistTrack(playlist, 0);
        ApiTrack track2 = testFixtures().insertPlaylistTrack(playlist, 1);
        ApiTrack track3 = testFixtures().insertPlaylistTrack(playlist, 2);

        List<Urn> trackUrns = command.with(playlist.getUrn()).call();

        assertThat(trackUrns).containsExactly(track1.getUrn(), track2.getUrn(), track3.getUrn());
    }

    @Test
    public void shouldNotLoadPlaylistTrackUrnsWithNoMetadata() throws Exception {
        ApiPlaylist playlist = testFixtures().insertPlaylist();
        ApiTrack track1 = testFixtures().insertPlaylistTrack(playlist, 0);
        ApiTrack track2 = testFixtures().insertPlaylistTrack(playlist, 1);

        // use a real playlist URN to test the join logic
        final Urn missingTrackUrn = testFixtures().insertPlaylist().getUrn();
        testFixtures().insertPlaylistTrack(playlist.getUrn(), missingTrackUrn, 2);

        List<Urn> trackUrns = command.with(playlist.getUrn()).call();

        assertThat(trackUrns).containsExactly(track1.getUrn(), track2.getUrn());
    }
}
