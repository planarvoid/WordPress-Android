package com.soundcloud.android.sync.posts;

import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.query.Query;
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
        final ApiPost apiTrackPost = ModelFixtures.apiTrackPost(apiTrack);
        testFixtures().insertTrackPost(apiTrackPost);
        testFixtures().insertPlaylistPost(2000L, 100L, false); // should not be removed

        command.call(singleton(apiTrackPost.toPropertySet()));

        assertThat(select(Query.from(Table.Posts.name())), counts(1));
    }

}
