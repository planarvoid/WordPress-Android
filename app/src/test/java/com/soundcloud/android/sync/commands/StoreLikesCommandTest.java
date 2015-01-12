package com.soundcloud.android.sync.commands;

import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.likes.ApiLike;
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
public class StoreLikesCommandTest extends StorageIntegrationTest {

    private StoreLikesCommand command;

    @Before
    public void setup() {
        command = new StoreLikesCommand(propeller());
    }

    @Test
    public void shouldInsertTrackAndPlaylistLikes() throws PropellerWriteException {
        final ApiLike trackLike = ModelFixtures.apiTrackLike();
        final ApiLike playlistLike = ModelFixtures.apiPlaylistLike();

        command.with(CollectionUtils.toPropertySets(Arrays.asList(trackLike, playlistLike))).call();

        assertThat(select(Query.from(Table.Likes.name())), counts(2));
        assertLikeInserted(trackLike, TableColumns.Sounds.TYPE_TRACK);
        assertLikeInserted(playlistLike, TableColumns.Sounds.TYPE_PLAYLIST);
    }

    private void assertLikeInserted(ApiLike like, int likeType) {
        assertThat(select(Query.from(Table.Likes.name())
                .whereEq(TableColumns.Likes._ID, like.getTargetUrn().getNumericId())
                .whereEq(TableColumns.Likes._TYPE, likeType)
                .whereEq(TableColumns.Likes.CREATED_AT, like.getCreatedAt().getTime())), counts(1));
    }

}