package com.soundcloud.android.sync.posts;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.util.Pair;

import java.util.Arrays;


@RunWith(SoundCloudTestRunner.class)
public class ReplacePlaylistCommandTest extends StorageIntegrationTest {

    private ReplacePlaylistCommand command;

    @Before
    public void setUp() throws Exception {
        command = new ReplacePlaylistCommand(propeller());
    }

    @Test
    public void shouldReplaceOldPlaylistMetadata() throws Exception {
        ApiPlaylist oldPlaylist = testFixtures().insertLocalPlaylist();
        ApiPlaylist newPlaylist = ModelFixtures.create(ApiPlaylist.class);

        command.with(Pair.create(oldPlaylist.getUrn(), newPlaylist)).call();

        databaseAssertions().assertPlaylistInserted(newPlaylist);
        databaseAssertions().assertPlaylistNotStored(oldPlaylist);
    }

    @Test
    public void shouldUpdatePlaylistTracksTableWithNewPlaylistId() throws Exception {
        ApiPlaylist oldPlaylist = testFixtures().insertLocalPlaylist();
        ApiTrack playlistTrack = testFixtures().insertPlaylistTrack(oldPlaylist, 0);
        ApiPlaylist newPlaylist = ModelFixtures.create(ApiPlaylist.class);

        command.with(Pair.create(oldPlaylist.getUrn(), newPlaylist)).call();

        databaseAssertions().assertPlaylistTracklist(newPlaylist.getId(), Arrays.asList(playlistTrack.getUrn()));
    }
}