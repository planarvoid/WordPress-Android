package com.soundcloud.android.sync.recommendations;

import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.storage.TableColumns;
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
        final List<ApiRecommendation> apiRecommendations = RecommendationsFixtures.createApiRecommendationsWithLikedReason(2);
        command.call(apiRecommendations);

        assertThat(select(from(RecommendationSeeds.TABLE)), counts(2));
        assertSeedInserted(apiRecommendations.get(0));
        assertSeedInserted(apiRecommendations.get(1));
    }

    @Test
    public void clearsRecommendationsBeforeInserting() {
        List<ApiRecommendation> apiRecommendations = RecommendationsFixtures.createApiRecommendationsWithLikedReason(1);
        command.call(apiRecommendations);

        assertSeedInserted(apiRecommendations.get(0));

        apiRecommendations = Collections.emptyList();
        command.call(apiRecommendations);

        assertThat(select(from(Recommendations.TABLE)), counts(0));
        assertThat(select(from(RecommendationSeeds.TABLE)), counts(0));
    }

    @Test
    public void insertsSeedAndRecommendedTracks() {
        final List<ApiRecommendation> apiRecommendations = RecommendationsFixtures.createApiRecommendationsWithLikedReason(1);
        command.call(apiRecommendations);

        ApiRecommendation recommendation = apiRecommendations.get(0);

        assertThat(select(from(RecommendationSeeds.TABLE)), counts(1));
        assertThat(select(from(Recommendations.TABLE)), counts(2));
        assertSeedInserted(recommendation);
        assertRecommendedTracksForSeedInserted(recommendation);
    }

    @Test
    public void unknownReasonsAreNotWritten() {
        final List<ApiRecommendation> apiRecommendations = new ArrayList<>();
        apiRecommendations.addAll(RecommendationsFixtures.createApiRecommendationsWithLikedReason(1));
        apiRecommendations.addAll(RecommendationsFixtures.createApiRecommendationsWithListenedToReason(1));
        apiRecommendations.addAll(RecommendationsFixtures.createApiRecommendationsWithUnknownReason(2));

        command.call(apiRecommendations);

        assertThat(select(from(RecommendationSeeds.TABLE)), counts(2));
        assertThat(select(from(Recommendations.TABLE)), counts(4));
        assertSeedInserted(apiRecommendations.get(0));
        assertSeedInserted(apiRecommendations.get(1));
    }

    private void assertSeedInserted(ApiRecommendation recommendation) {
        assertThat(select(from(RecommendationSeeds.TABLE)
                .whereEq(RecommendationSeeds.SEED_SOUND_ID, recommendation.getSeedTrack().getUrn().getNumericId())
                .whereEq(RecommendationSeeds.SEED_SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK)
                .whereEq(RecommendationSeeds.RECOMMENDATION_REASON, getReason(recommendation))), counts(1));

        databaseAssertions().assertTrackInserted(recommendation.getSeedTrack());
        databaseAssertions().assertUserInserted(recommendation.getSeedTrack().getUser());
    }

    private int getReason(ApiRecommendation apiRecommendation) {
        switch (apiRecommendation.getRecommendationReason()){
            case LIKED:
                return RecommendationSeeds.REASON_LIKED;
            case LISTENED_TO:
                return RecommendationSeeds.REASON_LISTENED_TO;
            default:
                throw new IllegalArgumentException("Unknown recommendation reason " + apiRecommendation.getRecommendationReason());
        }
    }

    private void assertRecommendedTracksForSeedInserted(ApiRecommendation recommendation) {
        assertThat(select(from(Recommendations.TABLE)), counts(2));
        for (ApiTrack track : recommendation.getRecommendations()) {
            databaseAssertions().assertTrackInserted(track);
            databaseAssertions().assertUserInserted(track.getUser());
        }
    }
}