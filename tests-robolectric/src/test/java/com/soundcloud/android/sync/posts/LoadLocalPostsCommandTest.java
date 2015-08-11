package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class LoadLocalPostsCommandTest extends StorageIntegrationTest {

    private LoadLocalPostsCommand command;

    @Test
    public void shouldLoadRepostedPlaylist() throws Exception {
        command = new LoadLocalPostsCommand(propeller(), TableColumns.Sounds.TYPE_PLAYLIST);
        ApiPlaylist playlist = testFixtures().insertPlaylist();
        testFixtures().insertPlaylistPost(playlist.getId(), 100L, true);

        List<PropertySet> postedPlaylists = command.call();

        expect(postedPlaylists).toContainExactly(PropertySet.from(
                PostProperty.TARGET_URN.bind(playlist.getUrn()),
                PostProperty.IS_REPOST.bind(true),
                PostProperty.CREATED_AT.bind(new Date(100L))
        ));
    }

    @Test
    public void shouldLoadRepostedTrack() throws Exception {
        command = new LoadLocalPostsCommand(propeller(), TableColumns.Sounds.TYPE_TRACK);
        ApiTrack track = testFixtures().insertTrack();
        testFixtures().insertTrackPost(track.getId(), 100L, true);

        List<PropertySet> postedTracks = command.call();

        expect(postedTracks).toContainExactly(PropertySet.from(
                PostProperty.TARGET_URN.bind(track.getUrn()),
                PostProperty.IS_REPOST.bind(true),
                PostProperty.CREATED_AT.bind(new Date(100L))
        ));
    }
}