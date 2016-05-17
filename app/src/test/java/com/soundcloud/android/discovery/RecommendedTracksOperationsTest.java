package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.sync.recommendations.StoreRecommendationsCommand;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RecommendedTracksOperationsTest extends AndroidUnitTest {
    private static final long SEED_ID = 1;
    private static final RecommendationReason REASON = RecommendationReason.LIKED;
    private final Scheduler scheduler = Schedulers.immediate();
    private final ApiTrack seedTrack = ModelFixtures.create(ApiTrack.class);
    private final List<ApiTrack> recommendedTracks = ModelFixtures.create(ApiTrack.class, 2);
    private final ApiTrack recommendedTrack = recommendedTracks.get(0);
    private final PublishSubject<Boolean> syncSubject = PublishSubject.create();

    @Mock private RecommendedTracksSyncInitiator recommendedTracksSyncInitiator;
    @Mock private RecommendationsStorage recommendationsStorage;
    @Mock private StoreRecommendationsCommand storeRecommendationsCommand;
    @Mock private FeatureFlags featureFlags;

    private RecommendedTracksOperations operations;
    private TestSubscriber<DiscoveryItem> subscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        operations = new RecommendedTracksOperations(recommendedTracksSyncInitiator,
                                                     recommendationsStorage,
                                                     storeRecommendationsCommand,
                                                     scheduler,
                                                     featureFlags);

        // setup happy path
        final PropertySet seed = createSeed();
        when(recommendationsStorage.seedTracks()).thenReturn(Observable.just(Collections.singletonList(seed)));
        when(recommendationsStorage.firstSeed()).thenReturn(Observable.just(seed));
        when(recommendationsStorage.recommendedTracksForSeed(SEED_ID)).thenReturn(Observable.just(
                createRecommendedTrackPropertySet()));
        when(recommendedTracksSyncInitiator.sync()).thenReturn(syncSubject);
        when(featureFlags.isEnabled(Flag.DISCOVERY_RECOMMENDATIONS)).thenReturn(true);
    }

    @Test
    public void loadsAllRecommendedTracksWithSeed() {
        final TestSubscriber<List<Urn>> testSubscriber = new TestSubscriber<>();
        final Urn recommendedTrackUrnOne = Urn.forTrack(2L);
        final Urn recommendedTrackUrnTwo = Urn.forTrack(3L);

        when(recommendationsStorage.recommendedTracksBeforeSeed(SEED_ID)).thenReturn(Observable.just(Collections.singletonList(
                recommendedTrackUrnOne)));
        when(recommendationsStorage.recommendedTracksAfterSeed(SEED_ID)).thenReturn(Observable.just(Collections.singletonList(
                recommendedTrackUrnTwo)));

        RecommendationBucket recommendationBucket = new RecommendationBucket(createSeed(), Collections.<Recommendation>emptyList());
        operations.tracksWithSeed(recommendationBucket).subscribe(testSubscriber);

        List<Urn> recommendedTracksWithSeed = testSubscriber.getOnNextEvents().get(0);

        assertThat(recommendedTracksWithSeed.get(0)).isEqualTo(recommendedTrackUrnOne);
        assertThat(recommendedTracksWithSeed.get(1)).isEqualTo(seedTrack.getUrn());
        assertThat(recommendedTracksWithSeed.get(2)).isEqualTo(recommendedTrackUrnTwo);
    }

    @Test
    public void loadsRecommendedTracksForSeed() {
        final TestSubscriber<List<TrackItem>> testSubscriber = new TestSubscriber<>();

        when(recommendationsStorage.recommendedTracksForSeed(SEED_ID)).thenReturn(Observable.just(
                createRecommendedTrackPropertySet()));

        operations.tracksForSeed(SEED_ID).subscribe(testSubscriber);

        List<TrackItem> recommendedTracksForSeed = testSubscriber.getOnNextEvents().get(0);
        TrackItem recommendedTrackItem = recommendedTracksForSeed.get(0);

        assertThat(recommendedTrackItem.getUrn()).isEqualTo(recommendedTrack.getUrn());
        assertThat(recommendedTrackItem.getTitle()).isEqualTo(recommendedTrack.getTitle());
        assertThat(recommendedTrackItem.getCreatorName()).isEqualTo(recommendedTrack.getUserName());
        assertThat(recommendedTrackItem.getDuration()).isEqualTo(recommendedTrack.getFullDuration());
    }

    @Test
    public void loadsAllRecommendedTracks() {
        final TestSubscriber<List<Urn>> testObserver = new TestSubscriber<>();
        final Urn recommendedTrackUrnOne = Urn.forTrack(2L);
        final Urn recommendedTrackUrnTwo = Urn.forTrack(3L);

        when(recommendationsStorage.recommendedTracks()).thenReturn(Observable.just(Arrays.asList(recommendedTrackUrnOne,
                recommendedTrackUrnTwo)));

        operations.allTracks().subscribe(testObserver);

        List<Urn> recommendedTracks = testObserver.getOnNextEvents().get(0);

        assertThat(recommendedTracks.size()).isEqualTo(2);
        assertThat(recommendedTracks.contains(recommendedTrackUrnOne));
        assertThat(recommendedTracks.contains(recommendedTrackUrnTwo));
    }

    @Test
    public void returnsEmptyObservableWhenFeatureFlagIsDisabled() {
        when(featureFlags.isEnabled(Flag.DISCOVERY_CHARTS)).thenReturn(false);

        operations.tracksBucket().subscribe(subscriber);

        subscriber.assertNoValues();
    }

    @Test
    public void waitsForSyncerToReturnData() {
        operations.tracksBucket().subscribe(subscriber);
        subscriber.assertNoValues();

        syncSubject.onNext(true);

        final List<DiscoveryItem> onNextEvents = subscriber.getOnNextEvents();
        subscriber.assertValueCount(1);

        assertRecommendedTrackItem(onNextEvents.get(0));
    }

    @Test
    public void returnsNoDataWhenSyncerHasFinishedAndResultIsEmpty() {
        when(recommendationsStorage.firstSeed()).thenReturn(Observable.<PropertySet>empty());
        operations.tracksBucket().subscribe(subscriber);
        syncSubject.onNext(true);

        subscriber.assertNoValues();
    }

    @Test
    public void cleanUpRecommendationsData() {
        operations.clearData();

        verify(storeRecommendationsCommand).clearTables();
        verify(recommendedTracksSyncInitiator).clearLastSyncTime();
    }

    private void assertRecommendedTrackItem(DiscoveryItem discoveryItem) {
        assertThat(discoveryItem.getKind()).isEqualTo(DiscoveryItem.Kind.TrackRecommendationItem);

        final RecommendationBucket recommendationBucket = (RecommendationBucket) discoveryItem;
        assertThat(recommendationBucket.getSeedTrackLocalId()).isEqualTo(SEED_ID);
        assertThat(recommendationBucket.getSeedTrackUrn()).isEqualTo(seedTrack.getUrn());
        assertThat(recommendationBucket.getSeedTrackTitle()).isEqualTo(seedTrack.getTitle());
        assertThat(recommendationBucket.getRecommendationReason()).isEqualTo(REASON);

        assertThat(recommendationBucket.getRecommendations().get(0).getTrack().getTitle()).isEqualTo(recommendedTracks.get(0)
                .getTitle());
        assertThat(recommendationBucket.getRecommendations().get(0).getTrack().getCreatorName()).isEqualTo(recommendedTracks.get(0)
                .getUserName());
    }

    private PropertySet createSeed() {
        return PropertySet.from(
                RecommendationProperty.SEED_TRACK_LOCAL_ID.bind(SEED_ID),
                RecommendationProperty.SEED_TRACK_URN.bind(seedTrack.getUrn()),
                RecommendationProperty.SEED_TRACK_TITLE.bind(seedTrack.getTitle()),
                RecommendationProperty.REASON.bind(REASON)
        );
    }

    private List<PropertySet> createRecommendedTrackPropertySet() {
        PropertySet trackPropertySet = PropertySet.from(
                RecommendedTrackProperty.SEED_SOUND_URN.bind(Urn.forTrack(SEED_ID)),
                PlayableProperty.URN.bind(Urn.forTrack(recommendedTrack.getId())),
                PlayableProperty.TITLE.bind(recommendedTrack.getTitle()),
                PlayableProperty.CREATOR_NAME.bind(recommendedTrack.getUserName()),
                TrackProperty.FULL_DURATION.bind(recommendedTrack.getFullDuration()),
                TrackProperty.SNIPPET_DURATION.bind(recommendedTrack.getSnippetDuration()),
                TrackProperty.SNIPPED.bind(false),
                TrackProperty.PLAY_COUNT.bind(recommendedTrack.getPlaybackCount()),
                PlayableProperty.LIKES_COUNT.bind(recommendedTrack.getLikesCount()),
                PlayableProperty.CREATED_AT.bind(recommendedTrack.getCreatedAt())
        );
        return Collections.singletonList(trackPropertySet);
    }
}
