package com.soundcloud.android.offline;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class LoadOfflinePlaylistsContainingTracksCommandTest extends StorageIntegrationTest {

    private LoadOfflinePlaylistsContainingTracksCommand command;

    @Before
    public void setUp() throws Exception {
        command = new LoadOfflinePlaylistsContainingTracksCommand(propeller());
    }

    @Test
    public void returnsOfflinePlaylistsForGivenTracks() {
        final List<Urn> tracks = new ArrayList<>();
        final List<Urn> playlists = new ArrayList<>();

        for (int i = 0; i < 1234; i++) {
            final Urn playlist = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();
            tracks.add(testFixtures().insertPlaylistTrack(playlist, 0).getUrn());
            playlists.add(playlist);
        }

        final List<Urn> actual = command.call(tracks);
        assertThat(actual).containsAll(playlists);
    }

    @Test
    public void ignorePlaylistNotMarkedForOffline() {
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        final Urn track = testFixtures().insertPlaylistTrack(playlist, 0).getUrn();

        final List<Urn> actual = command.call(singletonList(track));

        assertThat(actual).isEmpty();
    }

}
