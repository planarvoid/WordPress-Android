package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
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

        List<Urn> trackLikes = command.call();

        expect(trackLikes).toContainExactly(track.getUrn());
    }

    @Test
    public void shouldNotLoadPlaylistLikes() throws Exception {
        testFixtures().insertLikedPlaylist(new Date(100));

        List<Urn> playlistLikes = command.call();
        expect(playlistLikes).toBeEmpty();
    }

    @Test
    public void shouldNotLoadLikesThatHaveNoTrackMetaData() throws Exception {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        // insert a track like with the same ID as the playlist to test that we are joining on tracks only
        testFixtures().insertLike(apiPlaylist.getId(), TableColumns.Sounds.TYPE_TRACK, new Date());

        List<Urn> trackLikes = command.call();

        expect(trackLikes).toBeEmpty();
    }
}