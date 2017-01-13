package com.soundcloud.android.discovery.recommendations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.SyncOperations.Result;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
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

import java.util.Collections;
import java.util.List;

public class RecommendedTracksOperationsTest extends AndroidUnitTest {

    private static final long SEED_ID = 1;
    private static final RecommendationReason REASON = RecommendationReason.LIKED;
    private static final int QUERY_POSITION = 1;
    private static final Urn QUERY_URN = Urn.NOT_SET;
    private static final Scheduler SCHEDULER = Schedulers.immediate();
    private static final ApiTrack SEED_TRACK = ModelFixtures.create(ApiTrack.class);
    private static final List<ApiTrack> RECOMMENDED_TRACKS = ModelFixtures.create(ApiTrack.class, 2);
    private static final ApiTrack RECOMMENDED_TRACK = RECOMMENDED_TRACKS.get(0);
    private static final PublishSubject<Result> SYNC_SUBJECT = PublishSubject.create();
    private static final TrackQueueItem PLAY_QUEUE_ITEM = TestPlayQueueItem.createTrack(RECOMMENDED_TRACK.getUrn());

    @Mock private SyncOperations syncOperations;
    @Mock private RecommendationsStorage recommendationsStorage;
    @Mock private StoreRecommendationsCommand storeRecommendationsCommand;
    @Mock private PlayQueueManager playQueueManager;

    private RecommendedTracksOperations operations;
    private TestSubscriber<DiscoveryItem> subscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        operations = new RecommendedTracksOperations(syncOperations,
                                                     recommendationsStorage,
                                                     storeRecommendationsCommand,
                                                     playQueueManager,
                                                     SCHEDULER
        );

        // setup happy path
        final PropertySet seed = createSeed();
        when(recommendationsStorage.firstSeed()).thenReturn(Observable.just(seed));
        when(recommendationsStorage.allSeeds()).thenReturn(Observable.just(seed));
        when(recommendationsStorage.recommendedTracksForSeed(SEED_ID)).thenReturn(Observable.just(
                createRecommendedTrackPropertySet()));
        when(syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_TRACKS)).thenReturn(SYNC_SUBJECT);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM);
    }

    @Test
    public void loadsRecommendedTracksForSeed() {
        final TestSubscriber<List<TrackItem>> testSubscriber = new TestSubscriber<>();

        when(recommendationsStorage.recommendedTracksForSeed(SEED_ID)).thenReturn(Observable.just(
                createRecommendedTrackPropertySet()));

        operations.tracksForSeed(SEED_ID).subscribe(testSubscriber);

        List<TrackItem> recommendedTracksForSeed = testSubscriber.getOnNextEvents().get(0);
        TrackItem recommendedTrackItem = recommendedTracksForSeed.get(0);

        assertThat(recommendedTrackItem.getUrn()).isEqualTo(RECOMMENDED_TRACK.getUrn());
        assertThat(recommendedTrackItem.getTitle()).isEqualTo(RECOMMENDED_TRACK.getTitle());
        assertThat(recommendedTrackItem.getCreatorName()).isEqualTo(RECOMMENDED_TRACK.getUserName());
        assertThat(recommendedTrackItem.getDuration()).isEqualTo(RECOMMENDED_TRACK.getFullDuration());
    }

    @Test
    public void returnsEmptyWhenNoRecommendationdsFromTrackSeed() {
        final TestSubscriber<List<TrackItem>> testSubscriber = new TestSubscriber<>();

        when(recommendationsStorage.recommendedTracksForSeed(SEED_ID)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()));

        operations.tracksForSeed(SEED_ID).subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        testSubscriber.assertCompleted();
    }

    @Test
    public void waitsForSyncerToReturnData() {
        operations.recommendedTracks().subscribe(subscriber);
        subscriber.assertNoValues();

        SYNC_SUBJECT.onNext(Result.SYNCING);

        final List<DiscoveryItem> onNextEvents = subscriber.getOnNextEvents();
        subscriber.assertValueCount(1);

        assertRecommendedTrackItem(onNextEvents.get(0));
    }

    @Test
    public void returnsNoDataWhenSyncerHasFinishedAndResultIsEmpty() {
        when(recommendationsStorage.firstSeed()).thenReturn(Observable.<PropertySet>empty());
        operations.recommendedTracks().subscribe(subscriber);

        SYNC_SUBJECT.onNext(Result.SYNCING);

        subscriber.assertNoValues();
    }

    @Test
    public void cleanUpRecommendationsData() {
        operations.clearData();

        verify(storeRecommendationsCommand).clearTables();
    }

    @Test
    public void recommendationShouldBePlayingIfCurrentPlayQueueItem() throws Exception {
        operations.recommendedTracks().subscribe(subscriber);
        SYNC_SUBJECT.onNext(Result.SYNCING);

        final RecommendedTracksBucketItem bucket = (RecommendedTracksBucketItem) subscriber.getOnNextEvents().get(0);

        assertThat(bucket.getRecommendations().get(0).isPlaying()).isTrue();
    }

    @Test
    public void recommendationShouldNotBePlayingIfNotCurrentPlayQueueItem() throws Exception {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(987L)));

        operations.recommendedTracks().subscribe(subscriber);
        SYNC_SUBJECT.onNext(Result.SYNCING);

        final RecommendedTracksBucketItem bucket = (RecommendedTracksBucketItem) subscriber.getOnNextEvents().get(0);

        assertThat(bucket.getRecommendations().get(0).isPlaying()).isFalse();
    }

    @Test
    public void recommendationShouldNotBePlayingIfPlayQueueIsEmpty() throws Exception {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PlayQueueItem.EMPTY);

        operations.recommendedTracks().subscribe(subscriber);
        SYNC_SUBJECT.onNext(Result.SYNCING);

        final RecommendedTracksBucketItem bucket = (RecommendedTracksBucketItem) subscriber.getOnNextEvents().get(0);

        assertThat(bucket.getRecommendations().get(0).isPlaying()).isFalse();
    }

    private void assertRecommendedTrackItem(DiscoveryItem discoveryItem) {
        assertThat(discoveryItem.getKind()).isEqualTo(DiscoveryItem.Kind.RecommendedTracksItem);

        final RecommendedTracksBucketItem recommendationBucket = (RecommendedTracksBucketItem) discoveryItem;
        assertThat(recommendationBucket.getSeedTrackLocalId()).isEqualTo(SEED_ID);
        assertThat(recommendationBucket.getSeedTrackUrn()).isEqualTo(SEED_TRACK.getUrn());
        assertThat(recommendationBucket.getSeedTrackTitle()).isEqualTo(SEED_TRACK.getTitle());
        assertThat(recommendationBucket.getRecommendationReason()).isEqualTo(REASON);
        assertThat(recommendationBucket.getSeedTrackQueryPosition()).isEqualTo(QUERY_POSITION);
        assertThat(recommendationBucket.getQueryUrn()).isEqualTo(QUERY_URN);

        assertThat(recommendationBucket.getRecommendations()
                                       .get(0)
                                       .getTrack()
                                       .getTitle()).isEqualTo(RECOMMENDED_TRACKS.get(0).getTitle());
        assertThat(recommendationBucket.getRecommendations().get(0).getTrack().getCreatorName()).isEqualTo(
                RECOMMENDED_TRACKS.get(0).getUserName());
    }

    private PropertySet createSeed() {
        return PropertySet.from(
                RecommendationProperty.SEED_TRACK_LOCAL_ID.bind(SEED_ID),
                RecommendationProperty.SEED_TRACK_URN.bind(SEED_TRACK.getUrn()),
                RecommendationProperty.SEED_TRACK_TITLE.bind(SEED_TRACK.getTitle()),
                RecommendationProperty.REASON.bind(REASON),
                RecommendationProperty.QUERY_POSITION.bind(QUERY_POSITION),
                RecommendationProperty.QUERY_URN.bind(QUERY_URN)
        );
    }

    private List<PropertySet> createRecommendedTrackPropertySet() {
        PropertySet trackPropertySet = TestPropertySets.mandatoryTrackProperties().merge(PropertySet.from(
                RecommendedTrackProperty.SEED_SOUND_URN.bind(Urn.forTrack(SEED_ID)),
                PlayableProperty.URN.bind(Urn.forTrack(RECOMMENDED_TRACK.getId())),
                PlayableProperty.TITLE.bind(RECOMMENDED_TRACK.getTitle()),
                PlayableProperty.CREATOR_NAME.bind(RECOMMENDED_TRACK.getUserName()),
                TrackProperty.FULL_DURATION.bind(RECOMMENDED_TRACK.getFullDuration()),
                TrackProperty.SNIPPET_DURATION.bind(RECOMMENDED_TRACK.getSnippetDuration()),
                TrackProperty.SNIPPED.bind(false),
                TrackProperty.PLAY_COUNT.bind(RECOMMENDED_TRACK.getPlaybackCount()),
                PlayableProperty.LIKES_COUNT.bind(RECOMMENDED_TRACK.getLikesCount()),
                PlayableProperty.CREATED_AT.bind(RECOMMENDED_TRACK.getCreatedAt())
        ));
        return Collections.singletonList(trackPropertySet);
    }
}
