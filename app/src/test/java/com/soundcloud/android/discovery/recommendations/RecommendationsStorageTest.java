package com.soundcloud.android.discovery.recommendations;

import static com.soundcloud.android.storage.Tables.Recommendations;
import static java.util.Collections.singletonList;

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
import java.util.List;

public class RecommendationsStorageTest extends StorageIntegrationTest {

    private static final int QUERY_POSITION = 1;
    private static final Urn QUERY_URN = Urn.NOT_SET;

    private final TestSubscriber<PropertySet> subscriber = new TestSubscriber<>();
    private RecommendationsStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new RecommendationsStorage(propellerRx());
    }

    @Test
    public void shouldLoadFirstSeed() {
        ApiUser user = testFixtures().insertUser();
        PropertySet first = insertSeedTrack(ModelFixtures.create(ApiTrack.class), user, RecommendationReason.LIKED);
        insertSeedTrack(ModelFixtures.create(ApiTrack.class), user, RecommendationReason.PLAYED);
        TestSubscriber<PropertySet> firstSeedSubscriber = new TestSubscriber<>();

        storage.firstSeed().subscribe(firstSeedSubscriber);

        firstSeedSubscriber.assertReceivedOnNext(singletonList(first));
    }

    @Test
    public void shouldLoadAllSeeds() {
        ApiUser user = testFixtures().insertUser();
        final List<ApiTrack> tracks = ModelFixtures.create(ApiTrack.class, 2);
        PropertySet first = insertSeedTrack(tracks.get(0), user, RecommendationReason.LIKED);
        PropertySet second = insertSeedTrack(tracks.get(1), user, RecommendationReason.PLAYED);

        storage.allSeeds().subscribe(subscriber);

        subscriber.assertReceivedOnNext(Arrays.asList(first, second));
    }

    @Test
    public void shouldReturnEmptyObservableWhenNoFirstSeed() {
        TestSubscriber<PropertySet> firstSeedSubscriber = new TestSubscriber<>();

        storage.firstSeed().subscribe(firstSeedSubscriber);

        firstSeedSubscriber.assertNoValues();
    }


    @Test
    public void shouldLoadAllRecommendationsForSeedTrack() {
        final List<ApiTrack> recommendedTracks = ModelFixtures.create(ApiTrack.class, 1);
        final PropertySet recommendation = insertRecommendation(ModelFixtures.create(ApiTrack.class),
                                                                RecommendationReason.LIKED,
                                                                recommendedTracks);
        final TestSubscriber<List<PropertySet>> recommendedTracksSubscriber = new TestSubscriber<>();
        final long localSeedId = recommendation.get(RecommendationProperty.SEED_TRACK_LOCAL_ID);
        final Urn seedUrn = Urn.forTrack(localSeedId);
        final List<PropertySet> recommendedTracksForSeed = singletonList(recommendedTrack(seedUrn,
                                                                                          recommendedTracks.get(0)));

        storage.recommendedTracksForSeed(localSeedId).subscribe(recommendedTracksSubscriber);

        recommendedTracksSubscriber.assertReceivedOnNext(singletonList(recommendedTracksForSeed));
        recommendedTracksSubscriber.assertCompleted();
    }

    private PropertySet insertRecommendation(ApiTrack seedTrack,
                                             RecommendationReason reason,
                                             List<ApiTrack> recommendedTracks) {
        ApiUser user = testFixtures().insertUser();
        long seedId = insertSeedTrack(seedTrack, user, reason).get(RecommendationProperty.SEED_TRACK_LOCAL_ID);
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
                RecommendedTrackProperty.IMAGE_URL_TEMPLATE.bind(recommendedTracks.get(0).getImageUrlTemplate()),
                RecommendedTrackProperty.TITLE.bind(recommendedTracks.get(0).getTitle()),
                RecommendedTrackProperty.USERNAME.bind(recommendedTracks.get(0).getUserName())
        );
    }

    private PropertySet insertSeedTrack(ApiTrack seedTrack, ApiUser apiUser, RecommendationReason reason) {
        testFixtures().insertTrackWithUser(seedTrack, apiUser);

        ContentValues cv = new ContentValues();
        cv.put(RecommendationSeeds.SEED_SOUND_ID.name(), seedTrack.getUrn().getNumericId());
        cv.put(RecommendationSeeds.SEED_SOUND_TYPE.name(), TableColumns.Sounds.TYPE_TRACK);
        cv.put(RecommendationSeeds.RECOMMENDATION_REASON.name(), getDbReason(reason));
        cv.put(RecommendationSeeds.QUERY_POSITION.name(), QUERY_POSITION);
        cv.put(RecommendationSeeds.QUERY_URN.name(), QUERY_URN.toString());
        long seedId = testFixtures().insertInto(RecommendationSeeds.TABLE, cv);

        return PropertySet.from(
                RecommendationProperty.SEED_TRACK_LOCAL_ID.bind(seedId),
                RecommendationProperty.SEED_TRACK_URN.bind(seedTrack.getUrn()),
                RecommendationProperty.SEED_TRACK_TITLE.bind(seedTrack.getTitle()),
                RecommendationProperty.REASON.bind(reason),
                RecommendationProperty.QUERY_POSITION.bind(QUERY_POSITION),
                RecommendationProperty.QUERY_URN.bind(QUERY_URN)
        );
    }

    private int getDbReason(RecommendationReason reason) {
        switch (reason) {
            case LIKED:
                return RecommendationSeeds.REASON_LIKED;
            case PLAYED:
                return RecommendationSeeds.REASON_PLAYED;
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

    private PropertySet recommendedTrack(Urn seedTrackUrn, ApiTrack recommendedTrack) {
        return PropertySet.from(
                RecommendedTrackProperty.SEED_SOUND_URN.bind(seedTrackUrn),
                PlayableProperty.URN.bind(recommendedTrack.getUrn()),
                PlayableProperty.TITLE.bind(recommendedTrack.getTitle()),
                PlayableProperty.CREATOR_URN.bind(recommendedTrack.getUser().getUrn()),
                PlayableProperty.CREATOR_NAME.bind(recommendedTrack.getUserName()),
                TrackProperty.FULL_DURATION.bind(recommendedTrack.getFullDuration()),
                TrackProperty.SNIPPET_DURATION.bind(recommendedTrack.getSnippetDuration()),
                TrackProperty.PLAY_COUNT.bind(recommendedTrack.getPlaybackCount()),
                PlayableProperty.LIKES_COUNT.bind(recommendedTrack.getLikesCount()),
                PlayableProperty.CREATED_AT.bind(recommendedTrack.getCreatedAt()),
                TrackProperty.SUB_HIGH_TIER.bind(recommendedTrack.isSubHighTier().get()),
                TrackProperty.SNIPPED.bind(recommendedTrack.isSnipped())
        );
    }
}
