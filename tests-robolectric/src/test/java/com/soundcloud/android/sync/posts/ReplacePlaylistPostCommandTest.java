package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.WriteResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.util.Pair;

import java.util.Arrays;


@RunWith(SoundCloudTestRunner.class)
public class ReplacePlaylistPostCommandTest extends StorageIntegrationTest {

    private ReplacePlaylistPostCommand command;

    @Before
    public void setUp() throws Exception {
        command = new ReplacePlaylistPostCommand(propeller());
    }

    @Test
    public void shouldReplaceOldPlaylistMetadata() throws Exception {
        ApiPlaylist oldPlaylist = testFixtures().insertLocalPlaylist();
        ApiPlaylist newPlaylist = ModelFixtures.create(ApiPlaylist.class);

        WriteResult result = command.with(Pair.create(oldPlaylist.getUrn(), newPlaylist)).call();
        expect(result.success()).toBeTrue();

        databaseAssertions().assertPlaylistInserted(newPlaylist);
        databaseAssertions().assertPlaylistNotStored(oldPlaylist);
    }

    @Test
    public void shouldUpdatePlaylistTracksTableWithNewPlaylistId() throws Exception {
        ApiPlaylist oldPlaylist = testFixtures().insertLocalPlaylist();
        ApiTrack playlistTrack = testFixtures().insertPlaylistTrack(oldPlaylist, 0);
        ApiPlaylist newPlaylist = ModelFixtures.create(ApiPlaylist.class);

        WriteResult result = command.with(Pair.create(oldPlaylist.getUrn(), newPlaylist)).call();
        expect(result.success()).toBeTrue();

        databaseAssertions().assertPlaylistTracklist(newPlaylist.getId(), Arrays.asList(playlistTrack.getUrn()));
    }

    @Test
    public void shouldUpdatePostsTableWithNewPlaylistId() throws Exception {
        ApiPlaylist oldPlaylist = testFixtures().insertLocalPlaylist();
        ApiPlaylist newPlaylist = ModelFixtures.create(ApiPlaylist.class);
        testFixtures().insertPlaylistPost(oldPlaylist.getId(), 123L, false);

        WriteResult result = command.with(Pair.create(oldPlaylist.getUrn(), newPlaylist)).call();
        System.out.println(result.getFailure());
        expect(result.success()).toBeTrue();

        databaseAssertions().assertPlaylistPostInsertedFor(newPlaylist);
    }
}