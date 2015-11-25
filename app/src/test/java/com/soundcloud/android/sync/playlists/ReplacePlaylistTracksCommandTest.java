package com.soundcloud.android.sync.playlists;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ReplacePlaylistTracksCommandTest extends StorageIntegrationTest {

    private ReplacePlaylistTracksCommand command;

    private ApiPlaylist apiPlaylist;

    @Before
    public void setup() {
        command = new ReplacePlaylistTracksCommand(propeller());
        apiPlaylist = testFixtures().insertPlaylist();
    }

    @Test
    public void persistsPlaylistTracksInDatabase() throws Exception {
        final List<Urn> newTracklist = Arrays.asList(Urn.forTrack(123L), Urn.forTrack(456L));

        assertThat(command.with(apiPlaylist.getUrn()).with(newTracklist).call().success()).isTrue();

        databaseAssertions().assertPlaylistTracklist(apiPlaylist.getId(), newTracklist);
    }

    @Test
    public void replacesPlaylistTracksInDatabase() throws Exception {
        // dummy tracks to be replaced
        testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        testFixtures().insertPlaylistTrack(apiPlaylist, 1);
        testFixtures().insertPlaylistTrack(apiPlaylist, 2);

        final List<Urn> newTracklist = Arrays.asList(Urn.forTrack(123L), Urn.forTrack(456L));

        assertThat(command.with(apiPlaylist.getUrn()).with(newTracklist).call().success()).isTrue();

        databaseAssertions().assertPlaylistTracklist(apiPlaylist.getId(), newTracklist);
    }
}
