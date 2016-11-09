package com.soundcloud.android.discovery.recommendedplaylists;

import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Where;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

public class StoreRecommendedPlaylistsCommandTest extends StorageIntegrationTest {

    private StoreRecommendedPlaylistsCommand command;

    @Before
    public void setUp() throws Exception {
        PropellerDatabase propeller = propeller();
        command = new StoreRecommendedPlaylistsCommand(propeller, new StorePlaylistsCommand(propeller, new StoreUsersCommand(propeller)));
    }

    @Test
    public void shouldStoreEmptyRecommendedPlaylists() {
        final List<ApiPlaylist> apiPlaylists = Collections.emptyList();
        final ApiRecommendedPlaylistBucket chill = ApiRecommendedPlaylistBucket.create("chill",
                                                                                       "Chill-Out",
                                                                                       null,
                                                                                       new ModelCollection<>(apiPlaylists));

        command.call(singletonList(chill));

        assertRecommendedPlaylistBucketInserted(chill);
    }

    @Test
    public void shouldStoreRecommendedPlaylists() {
        final List<ApiPlaylist> apiPlaylists = Collections.singletonList(ModelFixtures.create(ApiPlaylist.class));
        final ApiRecommendedPlaylistBucket chill = ApiRecommendedPlaylistBucket.create("chill",
                                                                                       "Chill-Out",
                                                                                       "http://www.soundcloud.com/mostbeautifulartworkever.bmp/",
                                                                                       new ModelCollection<>(apiPlaylists));

        command.call(singletonList(chill));

        assertRecommendedPlaylistBucketInserted(chill);
    }

    @Test
    public void shouldDeleteOldRecommendedPlaylists() throws Exception {
        final List<ApiPlaylist> oldPlaylists = Collections.singletonList(ModelFixtures.create(ApiPlaylist.class));
        final List<ApiPlaylist> newPlaylists = Collections.singletonList(ModelFixtures.create(ApiPlaylist.class));
        final ApiRecommendedPlaylistBucket oldBucket = ApiRecommendedPlaylistBucket.create("oldBucket",
                                                                                           "Chill-Out",
                                                                                           "http://www.soundcloud.com/mostbeautifulartworkever.bmp/",
                                                                                           new ModelCollection<>(oldPlaylists));

        final ApiRecommendedPlaylistBucket newBucket = ApiRecommendedPlaylistBucket.create("newBucket",
                                                                                           "Chill-Out",
                                                                                           "http://www.soundcloud.com/mostbeautifulartworkever.bmp/",
                                                                                           new ModelCollection<>(newPlaylists));

        command.call(singletonList(oldBucket));
        command.call(singletonList(newBucket));

        assertRecommendedPlaylistBucketRemoved(oldBucket);
        assertRecommendedPlaylistBucketInserted(newBucket);
    }

    private void assertRecommendedPlaylistBucketRemoved(ApiRecommendedPlaylistBucket bucket) {
        assertThat(
                select(
                        from(Tables.RecommendedPlaylistBucket.TABLE)
                                .whereEq(Tables.RecommendedPlaylistBucket.KEY, bucket.key())
                )
        ).counts(0);

        for (ApiPlaylist apiPlaylist : bucket.playlists()) {
            assertThat(
                    select(
                            from(Tables.RecommendedPlaylist.TABLE)
                                    .whereEq(Tables.RecommendedPlaylist.PLAYLIST_ID,
                                             apiPlaylist.getUrn().getNumericId())
                    )
            ).counts(0);
        }
    }

    private void assertRecommendedPlaylistBucketInserted(ApiRecommendedPlaylistBucket bucket) {
        Where artworkCondition;
        if (bucket.artworkUrl().isPresent()) {
            artworkCondition = filter().whereEq(Tables.RecommendedPlaylistBucket.ARTWORK_URL,
                                                bucket.artworkUrl().get());
        } else {
            artworkCondition = filter().whereNull(Tables.RecommendedPlaylistBucket.ARTWORK_URL);
        }

        assertThat(
                select(
                        from(Tables.RecommendedPlaylistBucket.TABLE)
                                .whereEq(Tables.RecommendedPlaylistBucket.KEY, bucket.key())
                                .whereEq(Tables.RecommendedPlaylistBucket.DISPLAY_NAME, bucket.displayName())
                                .where(artworkCondition)
                )
        ).counts(1);

        for (ApiPlaylist apiPlaylist : bucket.playlists()) {
            assertThat(
                    select(
                            from(Tables.RecommendedPlaylist.TABLE)
                                    .whereEq(Tables.RecommendedPlaylist.PLAYLIST_ID,
                                             apiPlaylist.getUrn().getNumericId())
                    )
            ).counts(1);
        }
    }

}
