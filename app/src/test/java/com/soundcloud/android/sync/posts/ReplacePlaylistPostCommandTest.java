package com.soundcloud.android.sync.posts;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.WriteResult;
import org.junit.Before;
import org.junit.Test;

import android.util.Pair;

import java.util.Collections;

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
        assertThat(result.success()).isTrue();

        databaseAssertions().assertPlaylistInserted(newPlaylist);
        databaseAssertions().assertPlaylistNotStored(oldPlaylist);
    }

    @Test
    public void shouldUpdatePlaylistTracksTableWithNewPlaylistId() throws Exception {
        ApiPlaylist oldPlaylist = testFixtures().insertLocalPlaylist();
        ApiTrack playlistTrack = testFixtures().insertPlaylistTrack(oldPlaylist, 0);
        ApiPlaylist newPlaylist = ModelFixtures.create(ApiPlaylist.class);

        WriteResult result = command.with(Pair.create(oldPlaylist.getUrn(), newPlaylist)).call();
        assertThat(result.success()).isTrue();

        databaseAssertions().assertPlaylistTracklist(newPlaylist.getId(), Collections.singletonList(playlistTrack.getUrn()));
    }

    @Test
    public void shouldUpdatePostsTableWithNewPlaylistId() throws Exception {
        ApiPlaylist oldPlaylist = testFixtures().insertLocalPlaylist();
        ApiPlaylist newPlaylist = ModelFixtures.create(ApiPlaylist.class);
        testFixtures().insertPlaylistPost(oldPlaylist.getId(), 123L, false);

        WriteResult result = command.with(Pair.create(oldPlaylist.getUrn(), newPlaylist)).call();
        assertThat(result.success()).isTrue();

        databaseAssertions().assertPlaylistPostInsertedFor(newPlaylist);
    }

    @Test
    public void shouldUpdateOfflineContentWithPlaylistId() throws Exception {
        ApiPlaylist oldPlaylist = testFixtures().insertLocalPlaylist();
        ApiPlaylist newPlaylist = ModelFixtures.create(ApiPlaylist.class);
        testFixtures().insertPlaylistPost(oldPlaylist.getId(), 123L, false);
        testFixtures().insertPlaylistMarkedForOfflineSync(oldPlaylist);

        WriteResult result = command.with(Pair.create(oldPlaylist.getUrn(), newPlaylist)).call();
        assertThat(result.success()).isTrue();

        databaseAssertions().assertPlaylistMarkedForOfflineSync(newPlaylist.getUrn());
    }

    @Test
    public void shouldNotUpdateOfflineContentWithPlaylistId() throws Exception {
        ApiPlaylist oldPlaylist = testFixtures().insertLocalPlaylist();
        ApiPlaylist newPlaylist = ModelFixtures.create(ApiPlaylist.class);
        testFixtures().insertPlaylistPost(oldPlaylist.getId(), 123L, false);

        WriteResult result = command.with(Pair.create(oldPlaylist.getUrn(), newPlaylist)).call();
        assertThat(result.success()).isTrue();

        databaseAssertions().assertPlaylistNotMarkedForOfflineSync(newPlaylist.getUrn());
    }
}