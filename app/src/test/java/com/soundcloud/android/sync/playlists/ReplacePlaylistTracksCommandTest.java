package com.soundcloud.android.sync.playlists;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
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

        expect(command.with(apiPlaylist.getUrn()).with(newTracklist).call().success()).toBeTrue();

        databaseAssertions().assertPlaylistTracklist(apiPlaylist.getId(), newTracklist);
    }

    @Test
    public void replacesPlaylistTracksInDatabase() throws Exception {
        // dummy tracks to be replaced
        testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        testFixtures().insertPlaylistTrack(apiPlaylist, 1);
        testFixtures().insertPlaylistTrack(apiPlaylist, 2);

        final List<Urn> newTracklist = Arrays.asList(Urn.forTrack(123L), Urn.forTrack(456L));

        expect(command.with(apiPlaylist.getUrn()).with(newTracklist).call().success()).toBeTrue();

        databaseAssertions().assertPlaylistTracklist(apiPlaylist.getId(), newTracklist);
    }
}