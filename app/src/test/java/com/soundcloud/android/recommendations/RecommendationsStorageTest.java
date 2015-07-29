package com.soundcloud.android.recommendations;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import android.content.ContentValues;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RecommendationsStorageTest extends StorageIntegrationTest {

    private RecommendationsStorage storage;
    private TestSubscriber<List<PropertySet>> subscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        storage = new RecommendationsStorage(propellerRx());
    }

    @Test
    public void seedTracksReturnsSeedTracksFromStorage() {
        /**
         * We are only inserting 1 recommendation here, because the query relies on a MIN function that
         * does not seem to work in unit tests but works fine under normal usage
         */
        PropertySet first = insertRecommendation(ModelFixtures.create(ApiTrack.class), RecommendationReason.LIKED, ModelFixtures.create(ApiTrack.class, 1));
        PropertySet second = insertRecommendation(ModelFixtures.create(ApiTrack.class), RecommendationReason.LISTENED_TO, ModelFixtures.create(ApiTrack.class, 1));

        storage.seedTracks().subscribe(subscriber);

        subscriber.assertReceivedOnNext(Collections.singletonList(Arrays.asList(first, second)));
    }

    private PropertySet insertRecommendation(ApiTrack seedTrack, RecommendationReason reason, List<ApiTrack> recommendedTracks){
        long seedId = insertSeedTrack(seedTrack, reason);
        for (ApiTrack track : recommendedTracks){
            insertRecommendationTrack(track, seedId);
        }
        return PropertySet.from(
                SeedSoundProperty.URN.bind(seedTrack.getUrn()),
                SeedSoundProperty.TITLE.bind(seedTrack.getTitle()),
                SeedSoundProperty.RECOMMENDATION_COUNT.bind(recommendedTracks.size()),
                SeedSoundProperty.REASON.bind(reason),
                RecommendationProperty.URN.bind(recommendedTracks.get(0).getUrn()),
                RecommendationProperty.TITLE.bind(recommendedTracks.get(0).getTitle()),
                RecommendationProperty.USERNAME.bind(recommendedTracks.get(0).getUserName())
        );
    }

    private long insertSeedTrack(ApiTrack seedTrack, RecommendationReason reason) {
        testFixtures().insertUser(seedTrack.getUser());
        testFixtures().insertTrack(seedTrack);

        ContentValues cv = new ContentValues();
        cv.put(TableColumns.RecommendationSeeds.SEED_SOUND_ID, seedTrack.getUrn().getNumericId());
        cv.put(TableColumns.RecommendationSeeds.SEED_SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.RecommendationSeeds.RECOMMENDATION_REASON, getDbReason(reason));
        return testFixtures().insertInto(Table.RecommendationSeeds, cv);
    }

    private int getDbReason(RecommendationReason reason) {
        switch (reason) {
            case LIKED:
                return TableColumns.RecommendationSeeds.REASON_LIKED;
            case LISTENED_TO:
                return TableColumns.RecommendationSeeds.REASON_LISTENED_TO;
            default:
                throw new IllegalArgumentException("Unknown recommendation reason " + reason);
        }
    }

    private long insertRecommendationTrack(ApiTrack apiTrack, long seedId) {
        testFixtures().insertUser(apiTrack.getUser());
        testFixtures().insertTrack(apiTrack);

        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Recommendations.RECOMMENDED_SOUND_ID, apiTrack.getUrn().getNumericId());
        cv.put(TableColumns.Recommendations.RECOMMENDED_SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.Recommendations.SEED_ID, seedId);
        return testFixtures().insertInto(Table.Recommendations, cv);
    }
}