package com.soundcloud.android.sync.posts;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class LoadLocalPostsCommandTest extends StorageIntegrationTest {

    private LoadLocalPostsCommand command;

    @Test
    public void shouldLoadRepostedPlaylist() throws Exception {
        command = new LoadLocalPostsCommand(propeller(), Tables.Sounds.TYPE_PLAYLIST);
        ApiPlaylist playlist = testFixtures().insertPlaylist();
        testFixtures().insertPlaylistPost(playlist.getId(), 100L, true);

        List<PostRecord> postedPlaylists = command.call();

        assertThat(postedPlaylists).containsExactly(DatabasePostRecord.createRepost(playlist.getUrn(), new Date(100L)));
    }

    @Test
    public void shouldLoadRepostedTrack() throws Exception {
        command = new LoadLocalPostsCommand(propeller(), Tables.Sounds.TYPE_TRACK);
        ApiTrack track = testFixtures().insertTrack();
        testFixtures().insertTrackPost(track.getId(), 100L, true);

        List<PostRecord> postedTracks = command.call();

        assertThat(postedTracks).containsExactly(DatabasePostRecord.createRepost(track.getUrn(), new Date(100L)));
    }
}
