package com.soundcloud.android.discovery;

import static com.soundcloud.android.storage.Tables.Recommendations;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables.RecommendationSeeds;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import android.content.ContentValues;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RecommendationsStorageTest extends StorageIntegrationTest {

    private final TestSubscriber<List<PropertySet>> subscriber = new TestSubscriber<>();

    private RecommendationsStorage storage;

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

    @Test
    public void recommendedTracksReturnTracksFromStorage() {
        List<ApiTrack> recommendedTracks = ModelFixtures.create(ApiTrack.class, 1);
        PropertySet recommendation = insertRecommendation(ModelFixtures.create(ApiTrack.class), RecommendationReason.LIKED, recommendedTracks);

        final long localSeedId = recommendation.get(RecommendationProperty.SEED_TRACK_LOCAL_ID);
        final Urn seedUrn = Urn.forTrack(localSeedId);
        List<PropertySet> recommendedTracksForSeed = Collections.singletonList(mapRecommendedTrack(seedUrn, recommendedTracks.get(0)));

        storage.recommendedTracksForSeed(localSeedId).subscribe(subscriber);

        subscriber.assertReceivedOnNext(Collections.singletonList(recommendedTracksForSeed));
        subscriber.assertCompleted();
    }

    @Test
    public void recommendedTrackUrnsReturnTrackUrnsFromStorage() {
        final TestSubscriber<List<Urn>> testSubscriber = new TestSubscriber<>();
        List<ApiTrack> apiTrackList = ModelFixtures.create(ApiTrack.class, 2);
        final ApiTrack seedTrack = apiTrackList.get(0);
        final ApiTrack recommendedTrack = apiTrackList.get(1);
        List<Urn> recommendedTrackUrns = Collections.singletonList(recommendedTrack.getUrn());
        insertRecommendation(seedTrack, RecommendationReason.LIKED, Collections.singletonList(recommendedTrack));

        storage.recommendedTracks().subscribe(testSubscriber);

        testSubscriber.assertReceivedOnNext(Collections.singletonList(recommendedTrackUrns));
        testSubscriber.assertCompleted();
    }

    @Test
    public void returnsRecommendedTracksPreviousToSeedFromStorage() {
        final TestSubscriber<List<Urn>> testSubscriber = new TestSubscriber<>();

        final List<ApiTrack> recommendedTracksPreviousToSeed = ModelFixtures.create(ApiTrack.class, 1);
        insertRecommendation(ModelFixtures.create(ApiTrack.class), RecommendationReason.LIKED, recommendedTracksPreviousToSeed);

        final List<ApiTrack> recommendedTracksSubsequentToSeed = ModelFixtures.create(ApiTrack.class, 1);
        PropertySet recommendation = insertRecommendation(ModelFixtures.create(ApiTrack.class), RecommendationReason.LIKED, recommendedTracksSubsequentToSeed);

        final long localSeedId = recommendation.get(RecommendationProperty.SEED_TRACK_LOCAL_ID);
        storage.recommendedTracksPreviousToSeed(localSeedId).subscribe(testSubscriber);

        final List<Urn> expectedRecommendedUrns = Collections.singletonList(recommendedTracksPreviousToSeed.get(0).getUrn());

        testSubscriber.assertReceivedOnNext(Collections.singletonList(expectedRecommendedUrns));
        testSubscriber.assertCompleted();
    }

    @Test
    public void returnsRecommendedTracksSubsequentToSeedFromStorage() {
        final TestSubscriber<List<Urn>> testSubscriber = new TestSubscriber<>();

        final List<ApiTrack> recommendedTracksPreviousToSeed = ModelFixtures.create(ApiTrack.class, 1);
        insertRecommendation(ModelFixtures.create(ApiTrack.class), RecommendationReason.LIKED, recommendedTracksPreviousToSeed);

        final List<ApiTrack> recommendedTracksSubsequentToSeed = ModelFixtures.create(ApiTrack.class, 1);
        PropertySet recommendation = insertRecommendation(ModelFixtures.create(ApiTrack.class), RecommendationReason.LIKED, recommendedTracksSubsequentToSeed);

        final long localSeedId = recommendation.get(RecommendationProperty.SEED_TRACK_LOCAL_ID);
        storage.recommendedTracksSubsequentToSeed(localSeedId).subscribe(testSubscriber);

        final List<Urn> expectedRecommendedUrns = Collections.singletonList(recommendedTracksSubsequentToSeed.get(0).getUrn());

        testSubscriber.assertReceivedOnNext(Collections.singletonList(expectedRecommendedUrns));
        testSubscriber.assertCompleted();
    }

    private PropertySet insertRecommendation(ApiTrack seedTrack, RecommendationReason reason, List<ApiTrack> recommendedTracks) {
        ApiUser user = testFixtures().insertUser();
        long seedId = insertSeedTrack(seedTrack, user, reason);
        for (ApiTrack track : recommendedTracks) {
            insertRecommendedTrack(track, user, seedId);
        }
        return PropertySet.from(
                RecommendationProperty.SEED_TRACK_LOCAL_ID.bind(seedId),
                RecommendationProperty.SEED_TRACK_URN.bind(seedTrack.getUrn()),
                RecommendationProperty.SEED_TRACK_TITLE.bind(seedTrack.getTitle()),
                RecommendationProperty.RECOMMENDED_TRACKS_COUNT.bind(recommendedTracks.size()),
                RecommendationProperty.REASON.bind(reason),
                RecommendedTrackProperty.URN.bind(recommendedTracks.get(0).getUrn()),
                RecommendedTrackProperty.TITLE.bind(recommendedTracks.get(0).getTitle()),
                RecommendedTrackProperty.USERNAME.bind(recommendedTracks.get(0).getUserName())
        );
    }

    private long insertSeedTrack(ApiTrack seedTrack, ApiUser apiUser, RecommendationReason reason) {
        testFixtures().insertTrackWithUser(seedTrack, apiUser);

        ContentValues cv = new ContentValues();
        cv.put(RecommendationSeeds.SEED_SOUND_ID.name(), seedTrack.getUrn().getNumericId());
        cv.put(RecommendationSeeds.SEED_SOUND_TYPE.name(), TableColumns.Sounds.TYPE_TRACK);
        cv.put(RecommendationSeeds.RECOMMENDATION_REASON.name(), getDbReason(reason));
        return testFixtures().insertInto(RecommendationSeeds.TABLE, cv);
    }

    private int getDbReason(RecommendationReason reason) {
        switch (reason) {
            case LIKED:
                return RecommendationSeeds.REASON_LIKED;
            case LISTENED_TO:
                return RecommendationSeeds.REASON_LISTENED_TO;
            default:
                throw new IllegalArgumentException("Unknown recommendation reason " + reason);
        }
    }

    private long insertRecommendedTrack(ApiTrack apiTrack, ApiUser user, long seedId) {
        testFixtures().insertTrackWithUser(apiTrack, user);

        ContentValues cv = new ContentValues();
        cv.put(Recommendations.RECOMMENDED_SOUND_ID.name(), apiTrack.getUrn().getNumericId());
        cv.put(Recommendations.RECOMMENDED_SOUND_TYPE.name(), TableColumns.Sounds.TYPE_TRACK);
        cv.put(Recommendations.SEED_ID.name(), seedId);
        return testFixtures().insertInto(Recommendations.TABLE, cv);
    }

    private PropertySet mapRecommendedTrack(Urn seedTrackUrn, ApiTrack recommendedTrack) {
        return PropertySet.from(
                RecommendedTrackProperty.SEED_SOUND_URN.bind(seedTrackUrn),
                PlayableProperty.URN.bind(recommendedTrack.getUrn()),
                PlayableProperty.TITLE.bind(recommendedTrack.getTitle()),
                PlayableProperty.CREATOR_NAME.bind(recommendedTrack.getUserName()),
                PlayableProperty.DURATION.bind(recommendedTrack.getDuration()),
                TrackProperty.PLAY_COUNT.bind(recommendedTrack.getPlaybackCount()),
                PlayableProperty.LIKES_COUNT.bind(recommendedTrack.getLikesCount()),
                PlayableProperty.CREATED_AT.bind(recommendedTrack.getCreatedAt())
        );
    }
}