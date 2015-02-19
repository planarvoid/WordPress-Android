package com.soundcloud.android.sync.likes;

import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.CollectionUtils;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.query.Query;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class RemoveLikesCommandTest extends StorageIntegrationTest {

    private RemoveLikesCommand command;

    @Before
    public void setup() {
        command = new RemoveLikesCommand(propeller(), TableColumns.Sounds.TYPE_TRACK);
    }

    @Test
    public void shouldRemoveLikes() throws PropellerWriteException {
        final ApiLike trackLike = testFixtures().insertTrackLike();
        final ApiLike trackLike2 = testFixtures().insertTrackLike();

        command.with(CollectionUtils.toPropertySets(Arrays.asList(trackLike, trackLike2))).call();

        assertThat(select(Query.from(Table.Likes.name())), counts(0));
    }

    @Test
    public void shouldRemoveJustTrackLikeWhenIdsAreTheSame() throws PropellerWriteException {
        ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        ApiPlaylist apiPlaylist = ModelFixtures.create(ApiPlaylist.class);
        apiPlaylist.setId(apiTrack.getId());

        final ApiLike trackLike = ModelFixtures.apiTrackLike(apiTrack);
        testFixtures().insertLike(trackLike);

        final ApiLike playlistLike = ModelFixtures.apiPlaylistLike(apiPlaylist);
        testFixtures().insertLike(playlistLike);

        command.with(CollectionUtils.toPropertySets(Arrays.asList(trackLike))).call();

        assertThat(select(Query.from(Table.Likes.name())), counts(1));
    }

}