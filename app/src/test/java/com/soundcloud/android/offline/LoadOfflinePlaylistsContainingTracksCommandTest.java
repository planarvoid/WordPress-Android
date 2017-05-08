package com.soundcloud.android.offline;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class LoadOfflinePlaylistsContainingTracksCommandTest extends StorageIntegrationTest {

    private LoadOfflinePlaylistsContainingTracksCommand command;

    @Before
    public void setUp() throws Exception {
        command = new LoadOfflinePlaylistsContainingTracksCommand(propeller());
    }

    @Test
    public void returnsOfflinePlaylistsForGivenTracks() {
        testFixtures().insertPlaylistTrack(testFixtures().insertPlaylistMarkedForOfflineSync(), 0);
        final Urn expectedPlaylist1 = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();
        final Urn expectedPlaylist2 = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();
        final Urn trackForPlaylist1 = testFixtures().insertPlaylistTrack(expectedPlaylist1, 0).getUrn();
        final Urn trackForPlaylist2 = testFixtures().insertPlaylistTrack(expectedPlaylist2, 0).getUrn();

        final List<Urn> actual = command.call(asList(trackForPlaylist1, trackForPlaylist2));

        assertThat(actual).containsExactly(expectedPlaylist1, expectedPlaylist2);
    }

    @Test
    public void ignorePlaylistNotMarkedForOffline() {
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        final Urn track = testFixtures().insertPlaylistTrack(playlist, 0).getUrn();

        final List<Urn> actual = command.call(singletonList(track));

        assertThat(actual).isEmpty();
    }

}
