package com.soundcloud.android.likes;

import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.query.Query;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class LikesWriteStorageTest extends StorageIntegrationTest {

    private LikesWriteStorage storage;

    @Before
    public void setup() {
        storage = new LikesWriteStorage(propeller());
    }

    @Test
    public void shouldInsertTrackAndPlaylistLikes() {
        List<ApiLike> likes = Arrays.asList(ModelFixtures.create(ApiTrackLike.class), ModelFixtures.create(ApiPlaylistLike.class));

        storage.storeLikes(likes);

        final ApiLike trackLike = likes.get(0);
        final ApiLike playlistLike = likes.get(1);

        assertThat(select(Query.from(Table.Likes.name())), counts(2));
        assertLikeInserted(trackLike, TableColumns.Sounds.TYPE_TRACK);
        assertLikeInserted(playlistLike, TableColumns.Sounds.TYPE_PLAYLIST);
    }

    private void assertLikeInserted(ApiLike like, int likeType) {
        assertThat(select(Query.from(Table.Likes.name())
                .whereEq(TableColumns.Likes._ID, like.getUrn().getNumericId())
                .whereEq(TableColumns.Likes._TYPE, likeType)
                .whereEq(TableColumns.Likes.CREATED_AT, like.getCreatedAt().getTime())), counts(1));
    }

}