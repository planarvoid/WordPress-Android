package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.likes.LikeProperty.TARGET_URN;
import static com.soundcloud.android.storage.Tables.Likes.ADDED_AT;
import static com.soundcloud.android.storage.Tables.Likes.CREATED_AT;
import static com.soundcloud.android.storage.Tables.Likes.REMOVED_AT;
import static com.soundcloud.android.storage.Tables.Likes._ID;
import static com.soundcloud.android.storage.Tables.Likes._TYPE;
import static com.soundcloud.android.storage.Tables.Sounds.TYPE_TRACK;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiPlaylistLike;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiTrackLike;
import static com.soundcloud.android.utils.PropertySets.toPropertySets;
import static com.soundcloud.propeller.ContentValuesBuilder.values;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;
import static java.util.Arrays.asList;

import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.storage.Tables.Likes;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.PropellerWriteException;
import org.junit.Before;
import org.junit.Test;

public class StoreLikesCommandTest extends StorageIntegrationTest {

    private StoreLikesCommand command;

    @Before
    public void setup() {
        command = new StoreLikesCommand(propeller());
    }

    @Test
    public void shouldInsertTrackAndPlaylistLikes() throws PropellerWriteException {
        final ApiLike trackLike = apiTrackLike();
        final ApiLike playlistLike = apiPlaylistLike();

        command.call(toPropertySets(trackLike, playlistLike));

        assertThat(select(from(Likes.TABLE))).counts(2);
        databaseAssertions().assertLikeInserted(trackLike);
        databaseAssertions().assertLikeInserted(playlistLike);
    }

    // we need to make sure we always fully replace local likes, since during a sync, we might find that a pending
    // local removal has been readded remotely, so we need to drop the local variant
    @Test
    public void shouldReplaceExistingEntries() throws PropellerWriteException {
        final PropertySet trackLike = apiTrackLike().toPropertySet();
        command.call(asList(trackLike));
        // set the removal date
        propeller().update(Likes.TABLE, values()
                                   .put(REMOVED_AT, 123L)
                                   .put(ADDED_AT, 123L)
                                   .get(),
                           filter().whereEq("_id", trackLike.get(TARGET_URN).getNumericId()));

        // replace the like, removal date should disappear
        command.call(asList(trackLike));

        assertThat(select(from(Likes.TABLE))).counts(1);
        assertThat(select(from(Likes.TABLE)
                                  .whereEq(_ID, trackLike.get(TARGET_URN).getNumericId())
                                  .whereEq(_TYPE, TYPE_TRACK)
                                  .whereEq(CREATED_AT, trackLike.get(LikeProperty.CREATED_AT).getTime())
                                  .whereNull(ADDED_AT)
                                  .whereNull(REMOVED_AT))).counts(1);
    }
}
