package com.soundcloud.android.sync.posts;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.collections.PropertySet;
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

        List<PropertySet> postedPlaylists = command.call();

        assertThat(postedPlaylists).containsExactly(PropertySet.from(
                PostProperty.TARGET_URN.bind(playlist.getUrn()),
                PostProperty.IS_REPOST.bind(true),
                PostProperty.CREATED_AT.bind(new Date(100L))
        ));
    }

    @Test
    public void shouldLoadRepostedTrack() throws Exception {
        command = new LoadLocalPostsCommand(propeller(), Tables.Sounds.TYPE_TRACK);
        ApiTrack track = testFixtures().insertTrack();
        testFixtures().insertTrackPost(track.getId(), 100L, true);

        List<PropertySet> postedTracks = command.call();

        assertThat(postedTracks).containsExactly(PropertySet.from(
                PostProperty.TARGET_URN.bind(track.getUrn()),
                PostProperty.IS_REPOST.bind(true),
                PostProperty.CREATED_AT.bind(new Date(100L))
        ));
    }
}
