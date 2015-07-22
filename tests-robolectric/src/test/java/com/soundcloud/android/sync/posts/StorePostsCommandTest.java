package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.storage.TableColumns.Posts;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.query.Query;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class StorePostsCommandTest extends StorageIntegrationTest {

    private StorePostsCommand command;

    @Before
    public void setup() {
        command = new StorePostsCommand(propeller());
    }

    @Test
    public void shouldPersistPlaylistPostsInDatabase() throws Exception {
        final PropertySet playlistPost1 = createPlaylistPost(Urn.forPlaylist(123L), new Date(100L), true);
        final PropertySet playlistPost2 = createPlaylistPost(Urn.forPlaylist(456L), new Date(200L), false);
        final List<PropertySet> playlists = Arrays.asList(playlistPost1, playlistPost2);

        command.with(playlists).call();

        assertPlaylistPostInserted(playlistPost1);
        assertPlaylistPostInserted(playlistPost2);
    }

    private PropertySet createPlaylistPost(Urn urn, Date date, boolean isRepost) {
        return PropertySet.from(
                PostProperty.TARGET_URN.bind(urn),
                PostProperty.CREATED_AT.bind(date),
                PostProperty.IS_REPOST.bind(isRepost)
        );
    }

    private void assertPlaylistPostInserted(PropertySet playlistPost) {
        assertThat(select(Query.from(Table.Posts.name())
                .whereEq(Posts.TARGET_ID, playlistPost.get(PostProperty.TARGET_URN).getNumericId())
                .whereEq(Posts.TARGET_TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereEq(Posts.TYPE, playlistPost.get(PostProperty.IS_REPOST) ? Posts.TYPE_REPOST : Posts.TYPE_POST)
                .whereEq(Posts.CREATED_AT, playlistPost.get(PostProperty.CREATED_AT).getTime())), counts(1));
    }
}