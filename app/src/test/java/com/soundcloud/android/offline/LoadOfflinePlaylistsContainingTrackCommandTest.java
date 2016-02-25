package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class LoadOfflinePlaylistsContainingTrackCommandTest extends StorageIntegrationTest {

    private LoadOfflinePlaylistsContainingTrackCommand command;

    @Before
    public void setUp() throws Exception {
        command = new LoadOfflinePlaylistsContainingTrackCommand(propeller());
    }

    @Test
    public void returnsOfflinePlaylistsForGivenTrack() {
        testFixtures().insertPlaylistTrack(testFixtures().insertPlaylistMarkedForOfflineSync(), 0);
        final Urn expectedPlaylist1 = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();
        final Urn trackForPlaylist = testFixtures().insertPlaylistTrack(expectedPlaylist1, 0).getUrn();
        final Urn expectedPlaylist2 = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();
        testFixtures().insertPlaylistTrack(expectedPlaylist2, trackForPlaylist, 0);

        final List<Urn> actual = command.call(trackForPlaylist);

        assertThat(actual).containsExactly(expectedPlaylist1, expectedPlaylist2);
    }

    @Test
    public void ignorePlaylistNotMarkedForOffline() {
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        final Urn track = testFixtures().insertPlaylistTrack(playlist, 0).getUrn();

        final List<Urn> actual = command.call(track);
     
        assertThat(actual).isEmpty();
    }

}
