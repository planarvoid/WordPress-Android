package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.model.PostProperty.IS_REPOST;
import static com.soundcloud.android.model.PostProperty.TARGET_URN;
import static com.soundcloud.android.storage.TableColumns.Posts.CREATED_AT;
import static com.soundcloud.android.storage.TableColumns.Posts.TARGET_ID;
import static com.soundcloud.android.storage.TableColumns.Posts.TARGET_TYPE;
import static com.soundcloud.android.storage.TableColumns.Posts.TYPE;
import static com.soundcloud.android.storage.TableColumns.Posts.TYPE_POST;
import static com.soundcloud.android.storage.TableColumns.Posts.TYPE_REPOST;
import static com.soundcloud.android.storage.TableColumns.Sounds.TYPE_PLAYLIST;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;

import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

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

        command.call(playlists);

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
        assertThat(select(from(Table.Posts.name())
                .whereEq(TARGET_ID, playlistPost.get(TARGET_URN).getNumericId())
                .whereEq(TARGET_TYPE, TYPE_PLAYLIST)
                .whereEq(TYPE, playlistPost.get(IS_REPOST) ? TYPE_REPOST : TYPE_POST)
                .whereEq(CREATED_AT, playlistPost.get(PostProperty.CREATED_AT).getTime()))).counts(1);
    }
}
