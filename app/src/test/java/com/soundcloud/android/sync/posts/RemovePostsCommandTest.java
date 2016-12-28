package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiTrackPost;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;
import static java.util.Collections.singleton;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.PropellerWriteException;
import org.junit.Before;
import org.junit.Test;

public class RemovePostsCommandTest extends StorageIntegrationTest {

    private RemovePostsCommand command;

    @Before
    public void setup() {
        command = new RemovePostsCommand(propeller());
    }

    @Test
    public void shouldRemoveTrackPosts() throws PropellerWriteException {
        final ApiTrack apiTrack = testFixtures().insertTrack();
        final ApiPost apiTrackPost = apiTrackPost(apiTrack);
        testFixtures().insertTrackPost(apiTrackPost);
        testFixtures().insertPlaylistPost(2000L, 100L, false); // should not be removed

        command.call(singleton(apiTrackPost));

        assertThat(select(from(Tables.Posts.TABLE))).counts(1);
    }

}
