package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playlists.RemoveTrackFromPlaylistCommand.RemoveTrackFromPlaylistParams;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class RemoveTrackFromPlaylistCommandTest extends StorageIntegrationTest {

    private RemoveTrackFromPlaylistCommand command;

    @Before
    public void setUp() throws Exception {
        command = new RemoveTrackFromPlaylistCommand(propeller());
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

    @Test
    public void removeAnotherTrackFromPlaylistWritesTrackToPlaylistTracksTable() {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        final ApiTrack track1 = testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        final ApiTrack track2 = testFixtures().insertPlaylistTrack(apiPlaylist, 1);
        final ApiTrack track3 = testFixtures().insertPlaylistTrackPendingRemoval(apiPlaylist, 2, new Date());

        command.call(new RemoveTrackFromPlaylistParams(apiPlaylist.getUrn(), track2.getUrn()));

        databaseAssertions().assertPlaylistTracklist(apiPlaylist.getId(), Arrays.asList(track1.getUrn()));
        databaseAssertions().assertPlaylistTrackForRemoval(apiPlaylist.getId(), track2.getUrn());
        databaseAssertions().assertPlaylistTrackForRemoval(apiPlaylist.getId(), track3.getUrn());
    }

    @Test
    public void removeAnotherTrackFromPlaylistReturnsUpdatedTrackCount() {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        final ApiTrack track1 = testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        testFixtures().insertPlaylistTrack(apiPlaylist, 1);
        testFixtures().insertPlaylistTrackPendingRemoval(apiPlaylist, 2, new Date());

        final Integer updatedCount = command.call(new RemoveTrackFromPlaylistParams(apiPlaylist.getUrn(), track1.getUrn()));

        expect(updatedCount).toEqual(1);
    }
}