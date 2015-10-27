package com.soundcloud.android.sync.likes;

import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.PropertySets;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.query.Query;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class StoreLikesCommandTest extends StorageIntegrationTest {

    private StoreLikesCommand command;

    @Before
    public void setup() {
        command = new StoreLikesCommand(propeller());
    }

    @Test
    public void shouldInsertTrackAndPlaylistLikes() throws PropellerWriteException {
        final ApiLike trackLike = ModelFixtures.apiTrackLike();
        final ApiLike playlistLike = ModelFixtures.apiPlaylistLike();

        command.call(PropertySets.toPropertySets(trackLike, playlistLike));

        assertThat(select(Query.from(Table.Likes.name())), counts(2));
        databaseAssertions().assertLikeInserted(trackLike);
        databaseAssertions().assertLikeInserted(playlistLike);
    }

    // we need to make sure we always fully replace local likes, since during a sync, we might find that a pending
    // local removal has been readded remotely, so we need to drop the local variant
    @Test
    public void shouldReplaceExistingEntries() throws PropellerWriteException {
        final PropertySet trackLike = ModelFixtures.apiTrackLike().toPropertySet();
        command.call(Arrays.asList(trackLike));
        // set the removal date
        propeller().update(Table.Likes, ContentValuesBuilder.values()
                        .put(TableColumns.Likes.REMOVED_AT, 123L)
                        .put(TableColumns.Likes.ADDED_AT, 123L)
                        .get(),
                filter().whereEq("_id", trackLike.get(LikeProperty.TARGET_URN).getNumericId()));

        // replace the like, removal date should disappear
        command.call(Arrays.asList(trackLike));

        assertThat(select(Query.from(Table.Likes.name())), counts(1));
        assertThat(select(Query.from(Table.Likes.name())
                .whereEq(TableColumns.Likes._ID, trackLike.get(LikeProperty.TARGET_URN).getNumericId())
                .whereEq(TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_TRACK)
                .whereEq(TableColumns.Likes.CREATED_AT, trackLike.get(LikeProperty.CREATED_AT).getTime())
                .whereNull(TableColumns.Likes.ADDED_AT)
                .whereNull(TableColumns.Likes.REMOVED_AT)), counts(1));
    }
}
