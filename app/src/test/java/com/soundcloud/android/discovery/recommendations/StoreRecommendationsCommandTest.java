package com.soundcloud.android.discovery.recommendations;

import static com.soundcloud.android.discovery.recommendations.RecommendationsFixtures.createApiRecommendationsWithLikedReason;
import static com.soundcloud.android.discovery.recommendations.RecommendationsFixtures.createApiRecommendationsWithListenedToReason;
import static com.soundcloud.android.discovery.recommendations.RecommendationsFixtures.createApiRecommendationsWithUnknownReason;
import static com.soundcloud.android.storage.TableColumns.Sounds.TYPE_TRACK;
import static com.soundcloud.android.storage.Tables.RecommendationSeeds.RECOMMENDATION_REASON;
import static com.soundcloud.android.storage.Tables.RecommendationSeeds.SEED_SOUND_ID;
import static com.soundcloud.android.storage.Tables.RecommendationSeeds.SEED_SOUND_TYPE;
import static com.soundcloud.android.storage.Tables.RecommendationSeeds.TABLE;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.storage.Tables.RecommendationSeeds;
import com.soundcloud.android.storage.Tables.Recommendations;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StoreRecommendationsCommandTest extends StorageIntegrationTest {

    private StoreRecommendationsCommand command;

    @Before
    public void setUp() throws Exception {
        command = new StoreRecommendationsCommand(propeller());
    }

    @Test
    public void insertsSeedTracksWithKeysAndDependencies() {
        final ModelCollection<ApiRecommendation> apiRecommendations =
                new ModelCollection(createApiRecommendationsWithLikedReason(2), Collections.<String, Link>emptyMap());

        command.call(apiRecommendations);

        assertThat(select(from(TABLE))).counts(2);
        assertSeedInserted(apiRecommendations.getCollection().get(0));
        assertSeedInserted(apiRecommendations.getCollection().get(1));
    }

    @Test
    public void clearsRecommendationsBeforeInserting() {
        final ModelCollection<ApiRecommendation> apiRecommendations =
                new ModelCollection(createApiRecommendationsWithLikedReason(1), Collections.<String, Link>emptyMap());

        command.call(apiRecommendations);

        assertSeedInserted(apiRecommendations.getCollection().get(0));

        command.call(ModelCollection.EMPTY);

        assertThat(select(from(Recommendations.TABLE))).counts(0);
        assertThat(select(from(TABLE))).counts(0);
    }

    @Test
    public void insertsSeedAndRecommendedTracks() {
        final ModelCollection<ApiRecommendation> apiRecommendations =
                new ModelCollection(createApiRecommendationsWithLikedReason(1), Collections.<String, Link>emptyMap());

        command.call(apiRecommendations);

        ApiRecommendation recommendation = apiRecommendations.getCollection().get(0);

        assertThat(select(from(TABLE))).counts(1);
        assertThat(select(from(Recommendations.TABLE))).counts(2);
        assertSeedInserted(recommendation);
        assertRecommendedTracksForSeedInserted(recommendation);
    }

    @Test
    public void unknownReasonsAreNotWritten() {
        final List<ApiRecommendation> recommendationList = new ArrayList<>();
        recommendationList.addAll(createApiRecommendationsWithLikedReason(1));
        recommendationList.addAll(createApiRecommendationsWithListenedToReason(1));
        recommendationList.addAll(createApiRecommendationsWithUnknownReason(2));

        final ModelCollection<ApiRecommendation> apiRecommendations =
                new ModelCollection(recommendationList, Collections.<String, Link>emptyMap());

        command.call(apiRecommendations);

        assertThat(select(from(TABLE))).counts(2);
        assertThat(select(from(Recommendations.TABLE))).counts(4);
        assertSeedInserted(apiRecommendations.getCollection().get(0));
        assertSeedInserted(apiRecommendations.getCollection().get(1));
    }

    @Test
    public void clearRecommendationsData() {
        final ModelCollection<ApiRecommendation> apiRecommendations =
                new ModelCollection(createApiRecommendationsWithLikedReason(1), Collections.<String, Link>emptyMap());

        command.call(apiRecommendations);
        assertThat(select(from(TABLE))).counts(1);
        assertThat(select(from(Recommendations.TABLE))).counts(2);

        command.clearTables();
        assertThat(select(from(TABLE))).counts(0);
        assertThat(select(from(Recommendations.TABLE))).counts(0);
    }

    private void assertSeedInserted(ApiRecommendation recommendation) {
        assertThat(select(from(TABLE)
                                  .whereEq(SEED_SOUND_ID, recommendation.getSeedTrack().getUrn().getNumericId())
                                  .whereEq(SEED_SOUND_TYPE, TYPE_TRACK)
                                  .whereEq(RECOMMENDATION_REASON, getReason(recommendation)))).counts(1);

        databaseAssertions().assertTrackInserted(recommendation.getSeedTrack());
        databaseAssertions().assertUserInserted(recommendation.getSeedTrack().getUser());
    }

    private int getReason(ApiRecommendation apiRecommendation) {
        switch (apiRecommendation.getRecommendationReason()) {
            case LIKED:
                return RecommendationSeeds.REASON_LIKED;
            case LISTENED_TO:
                return RecommendationSeeds.REASON_PLAYED;
            default:
                throw new IllegalArgumentException("Unknown recommendation reason " + apiRecommendation.getRecommendationReason());
        }
    }

    private void assertRecommendedTracksForSeedInserted(ApiRecommendation recommendation) {
        assertThat(select(from(Recommendations.TABLE))).counts(2);
        for (ApiTrack track : recommendation.getRecommendations()) {
            databaseAssertions().assertTrackInserted(track);
            databaseAssertions().assertUserInserted(track.getUser());
        }
    }
}
