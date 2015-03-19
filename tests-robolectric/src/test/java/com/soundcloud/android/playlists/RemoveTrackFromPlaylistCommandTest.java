package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playlists.RemoveTrackFromPlaylistCommand.RemoveTrackFromPlaylistParams;
import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class RemoveTrackFromPlaylistCommandTest extends StorageIntegrationTest {

    RemoveTrackFromPlaylistCommand command;

    @Mock private Thread backgroundThread;

    @Before
    public void setUp() throws Exception {
        command = new RemoveTrackFromPlaylistCommand(propeller(), providerOf(backgroundThread));
    }

    @Test
    public void removeTrackFromPlaylistReturnsUpdatedTrackCount() {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        final ApiTrack track1 = testFixtures().insertPlaylistTrack(apiPlaylist, 0);

        final Integer updatedCount = command.call(new RemoveTrackFromPlaylistParams(apiPlaylist.getUrn(), track1.getUrn()));

        expect(updatedCount).toEqual(0);
    }

    @Test
    public void removeTrackFromPlaylistWritesTrackToPlaylistTracksTable() {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        final ApiTrack track1 = testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        final ApiTrack track2 = testFixtures().insertPlaylistTrack(apiPlaylist, 1);

        command.call(new RemoveTrackFromPlaylistParams(apiPlaylist.getUrn(), track1.getUrn()));

        databaseAssertions().assertPlaylistTracklist(apiPlaylist.getId(), Arrays.asList(track2.getUrn()));
        databaseAssertions().assertPlaylistTrackForRemoval(apiPlaylist.getId(), track1.getUrn());
    }
}