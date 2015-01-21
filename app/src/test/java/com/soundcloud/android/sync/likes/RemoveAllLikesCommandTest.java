package com.soundcloud.android.sync.likes;

import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.query.Query;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class RemoveAllLikesCommandTest extends StorageIntegrationTest {

    private RemoveAllLikesCommand command;

    @Before
    public void setup() {
        command = new RemoveAllLikesCommand(propeller());
    }

    @Test
    public void shouldRemoveLikes() throws PropellerWriteException {
        testFixtures().insertTrackLike();
        testFixtures().insertPlaylistLike();

        command.call();

        assertThat(select(Query.from(Table.Likes.name())), counts(0));
    }

}