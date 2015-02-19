package com.soundcloud.android.sync.likes;

import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
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
        command = new RemoveLikesCommand(propeller());
    }

    @Test
    public void shouldRemoveLikes() throws PropellerWriteException {
        final ApiLike trackLike = testFixtures().insertTrackLike();
        final ApiLike playlistLike = testFixtures().insertPlaylistLike();

        command.with(CollectionUtils.toPropertySets(Arrays.asList(trackLike, playlistLike))).call();

        assertThat(select(Query.from(Table.Likes.name())), counts(0));
    }

}