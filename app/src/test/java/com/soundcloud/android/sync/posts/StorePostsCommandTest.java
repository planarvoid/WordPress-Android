package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.storage.Tables.Posts.TYPE_POST;
import static com.soundcloud.android.storage.Tables.Posts.TYPE_REPOST;
import static com.soundcloud.android.storage.Tables.Sounds.TYPE_PLAYLIST;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables.Posts;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
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
        final PostRecord playlistPost2 = ApiPost.create(Urn.forPlaylist(456L), new Date(200L));
        final PostRecord playlistPost1 = ApiRepost.create(Urn.forPlaylist(123L), new Date(100L));
        final List<PostRecord> playlists = Arrays.asList(playlistPost1, playlistPost2);

        command.call(playlists);

        assertPlaylistPostInserted(playlistPost1);
        assertPlaylistPostInserted(playlistPost2);
    }

    private void assertPlaylistPostInserted(PostRecord postRecord) {
        assertThat(select(from(Posts.TABLE)
                               .whereEq(Posts.TARGET_ID, postRecord.getTargetUrn().getNumericId())
                               .whereEq(Posts.TARGET_TYPE, TYPE_PLAYLIST)
                               .whereEq(Posts.TYPE, postRecord.isRepost() ? TYPE_REPOST : TYPE_POST)
                               .whereEq(Posts.CREATED_AT, postRecord.getCreatedAt().getTime()))).counts(1);
    }
}
