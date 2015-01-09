package com.soundcloud.android.likes;

import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.CollectionUtils;
import com.soundcloud.propeller.query.Query;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class LikesWriteStorageTest extends StorageIntegrationTest {

    private LikesWriteStorage storage;

    @Before
    public void setup() {
        storage = new LikesWriteStorage(propeller());
    }

    @Test
    public void shouldInsertTrackAndPlaylistLikes() {
        final ApiLike trackLike = ModelFixtures.apiTrackLike();
        final ApiLike playlistLike = ModelFixtures.apiPlaylistLike();

        storage.storeLikes(CollectionUtils.toPropertySets(Arrays.asList(trackLike, playlistLike)));

        assertThat(select(Query.from(Table.Likes.name())), counts(2));
        assertLikeInserted(trackLike, TableColumns.Sounds.TYPE_TRACK);
        assertLikeInserted(playlistLike, TableColumns.Sounds.TYPE_PLAYLIST);
    }

    @Test
    public void shouldRemoveLikes() {
        final ApiLike trackLike = testFixtures().insertTrackLike();
        final ApiLike playlistLike = testFixtures().insertPlaylistLike();

        storage.removeLikes(CollectionUtils.toPropertySets(Arrays.asList(trackLike, playlistLike)));

        assertThat(select(Query.from(Table.Likes.name())), counts(0));
    }

    private void assertLikeInserted(ApiLike like, int likeType) {
        assertThat(select(Query.from(Table.Likes.name())
                .whereEq(TableColumns.Likes._ID, like.getTargetUrn().getNumericId())
                .whereEq(TableColumns.Likes._TYPE, likeType)
                .whereEq(TableColumns.Likes.CREATED_AT, like.getCreatedAt().getTime())), counts(1));
    }

}