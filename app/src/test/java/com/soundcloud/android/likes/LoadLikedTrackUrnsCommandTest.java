package com.soundcloud.android.likes;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class LoadLikedTrackUrnsCommandTest extends StorageIntegrationTest {

    private LoadLikedTrackUrnsCommand command;

    @Before
    public void setup() {
        command = new LoadLikedTrackUrnsCommand(propeller());
    }

    @Test
    public void shouldLoadTrackLikes() throws Exception {
        ApiTrack track = testFixtures().insertLikedTrack(new Date(100));
        testFixtures().insertLikedTrackPendingRemoval(new Date(200)); // must not be returned

        List<Urn> trackLikes = command.call(null);

        assertThat(trackLikes).containsExactly(track.getUrn());
    }

    @Test
    public void shouldNotLoadPlaylistLikes() throws Exception {
        testFixtures().insertLikedPlaylist(new Date(100));

        List<Urn> playlistLikes = command.call(null);
        assertThat(playlistLikes).isEmpty();
    }

    @Test
    public void shouldNotLoadLikesThatHaveNoTrackMetaData() throws Exception {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        // insert a track like with the same ID as the playlist to test that we are joining on tracks only
        testFixtures().insertLike(apiPlaylist.getId(), Tables.Sounds.TYPE_TRACK, new Date());

        List<Urn> trackLikes = command.call(null);

        assertThat(trackLikes).isEmpty();
    }
}
