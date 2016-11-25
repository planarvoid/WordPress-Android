package com.soundcloud.android.collection;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class LoadLikedTrackPreviewsCommandTest extends StorageIntegrationTest {

    private LoadLikedTrackPreviewsCommand command;

    @Before
    public void setUp() throws Exception {
        command = new LoadLikedTrackPreviewsCommand(propeller());
    }

    @Test
    public void shouldLoadPreviewsOfLikedTracksOrderedByDateDescending() {
        ApiTrack track1 = testFixtures().insertLikedTrack(new Date(0));
        ApiTrack track2 = testFixtures().insertLikedTrack(new Date(1000)); // newer; should come out on top
        List<LikedTrackPreview> expectedItems = asList(
                LikedTrackPreview.create(track2.getUrn(), track2.getImageUrlTemplate().get()),
                LikedTrackPreview.create(track1.getUrn(), track1.getImageUrlTemplate().get())
        );

        List<LikedTrackPreview> items = command.call(null);

        assertThat(items).isEqualTo(expectedItems);
    }

    @Test
    public void shouldSkipPreviewsOfLikesPendingRemoval() {
        testFixtures().insertLikedTrackPendingRemoval(new Date()); // must not be returned

        List<LikedTrackPreview> items = command.call(null);

        assertThat(items).isEmpty();
    }

    @Test
    public void shouldNotLoadPlaylistLikes() throws Exception {
        testFixtures().insertLikedPlaylist(new Date(100));

        List<LikedTrackPreview> items = command.call(null);
        assertThat(items).isEmpty();
    }

    @Test
    public void shouldNotLoadLikesThatHaveNoTrackMetaData() throws Exception {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        // insert a track like with the same ID as the playlist to test that we are joining on tracks only
        testFixtures().insertLike(apiPlaylist.getId(), Tables.Sounds.TYPE_TRACK, new Date());

        List<LikedTrackPreview> items = command.call(null);

        assertThat(items).isEmpty();
    }

}
