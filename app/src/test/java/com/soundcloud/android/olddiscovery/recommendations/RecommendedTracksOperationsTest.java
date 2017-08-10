package com.soundcloud.android.olddiscovery.recommendations;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.olddiscovery.OldDiscoveryItem;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.SyncOperations.Result;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRepository;
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import rx.Observable;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class RecommendedTracksOperationsTest {

    private static final long SEED_ID = 1;
    private static final RecommendationReason REASON = RecommendationReason.LIKED;
    private static final int QUERY_POSITION = 1;
    private static final Urn QUERY_URN = Urn.NOT_SET;
    private static final Scheduler SCHEDULER = Schedulers.immediate();
    private static final ApiTrack SEED_TRACK = ModelFixtures.create(ApiTrack.class);
    private static final List<TrackItem> RECOMMENDED_TRACKS = ModelFixtures.trackItems(2);
    private static final TrackItem RECOMMENDED_TRACK = RECOMMENDED_TRACKS.get(0);
    private static final PublishSubject<Result> SYNC_SUBJECT = PublishSubject.create();
    private static final TrackQueueItem PLAY_QUEUE_ITEM = TestPlayQueueItem.createTrack(RECOMMENDED_TRACK.getUrn());

    @Mock private SyncOperations syncOperations;
    @Mock private RecommendationsStorage recommendationsStorage;
    @Mock private StoreRecommendationsCommand storeRecommendationsCommand;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private TrackItemRepository trackRepository;

    private RecommendedTracksOperations operations;
    private TestSubscriber<OldDiscoveryItem> subscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        operations = new RecommendedTracksOperations(syncOperations,
                                                     recommendationsStorage,
                                                     storeRecommendationsCommand,
                                                     playQueueManager,
                                                     trackRepository,
                                                     SCHEDULER
        );

        // setup happy path
        final RecommendationSeed seed = createSeed();
        when(recommendationsStorage.firstSeed()).thenReturn(Observable.just(seed));
        when(recommendationsStorage.allSeeds()).thenReturn(Observable.just(seed));
        when(recommendationsStorage.recommendedTracksForSeed(SEED_ID)).thenReturn(Observable.just(
                Collections.singletonList(RECOMMENDED_TRACK.getUrn())));
        when(syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_TRACKS)).thenReturn(SYNC_SUBJECT);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM);
        when(trackRepository.trackListFromUrns(Collections.singletonList(RECOMMENDED_TRACK.getUrn()))).thenReturn(Single.just(RECOMMENDED_TRACKS));
    }

    @Test
    public void loadsRecommendedTracksForSeed() {
        final TestSubscriber<List<TrackItem>> testSubscriber = new TestSubscriber<>();

        when(recommendationsStorage.recommendedTracksForSeed(SEED_ID)).thenReturn(Observable.just(
                Collections.singletonList(RECOMMENDED_TRACK.getUrn())));

        operations.tracksForSeed(SEED_ID).subscribe(testSubscriber);

        List<TrackItem> recommendedTracksForSeed = testSubscriber.getOnNextEvents().get(0);
        TrackItem recommendedTrackItem = recommendedTracksForSeed.get(0);

        assertThat(recommendedTrackItem.getUrn()).isEqualTo(RECOMMENDED_TRACK.getUrn());
        assertThat(recommendedTrackItem.title()).isEqualTo(RECOMMENDED_TRACK.title());
        assertThat(recommendedTrackItem.creatorName()).isEqualTo(RECOMMENDED_TRACK.creatorName());
        assertThat(recommendedTrackItem.getDuration()).isEqualTo(RECOMMENDED_TRACK.fullDuration());
    }

    @Test
    public void returnsEmptyWhenNoRecommendationdsFromTrackSeed() {
        final TestSubscriber<List<TrackItem>> testSubscriber = new TestSubscriber<>();

        when(recommendationsStorage.recommendedTracksForSeed(SEED_ID)).thenReturn(Observable.just(Collections.emptyList()));

        operations.tracksForSeed(SEED_ID).subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        testSubscriber.assertCompleted();
    }

    @Test
    public void waitsForSyncerToReturnData() {
        operations.recommendedTracks().subscribe(subscriber);
        subscriber.assertNoValues();

        SYNC_SUBJECT.onNext(Result.SYNCING);

        final List<OldDiscoveryItem> onNextEvents = subscriber.getOnNextEvents();
        subscriber.assertValueCount(1);

        assertRecommendedTrackItem(onNextEvents.get(0));
    }

    @Test
    public void returnsNoDataWhenSyncerHasFinishedAndResultIsEmpty() {
        when(recommendationsStorage.firstSeed()).thenReturn(Observable.empty());
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

        assertThat(bucket.recommendations().get(0).isPlaying()).isTrue();
    }

    @Test
    public void recommendationShouldNotBePlayingIfNotCurrentPlayQueueItem() throws Exception {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(987L)));

        operations.recommendedTracks().subscribe(subscriber);
        SYNC_SUBJECT.onNext(Result.SYNCING);

        final RecommendedTracksBucketItem bucket = (RecommendedTracksBucketItem) subscriber.getOnNextEvents().get(0);

        assertThat(bucket.recommendations().get(0).isPlaying()).isFalse();
    }

    @Test
    public void recommendationShouldNotBePlayingIfPlayQueueIsEmpty() throws Exception {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PlayQueueItem.EMPTY);

        operations.recommendedTracks().subscribe(subscriber);
        SYNC_SUBJECT.onNext(Result.SYNCING);

        final RecommendedTracksBucketItem bucket = (RecommendedTracksBucketItem) subscriber.getOnNextEvents().get(0);

        assertThat(bucket.recommendations().get(0).isPlaying()).isFalse();
    }

    @Test
    public void allBucketsEmitsEmptyWhenNoSeeds() throws Exception {
        when(syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_TRACKS)).thenReturn(Observable.just(Result.SYNCED));
        when(recommendationsStorage.allSeeds()).thenReturn(Observable.empty());

        operations.allBuckets().test()
                  .assertNoValues()
                  .assertCompleted();
    }

    @Test
    public void allBucketsEmitsItems() throws Exception {
        int seedId = 1;
        Urn seedTrackUrn = Urn.forTrack(2);
        RecommendationSeed recommendationSeed = RecommendationSeed.create(seedId, seedTrackUrn, "Seed", RecommendationReason.LIKED, 0, Urn.NOT_SET);
        List<Urn> recommendedTracksUrns = Arrays.asList(Urn.forTrack(123), Urn.forTrack(456));
        List<TrackItem> recommendedTracks = Arrays.asList(ModelFixtures.trackItem(Urn.forTrack(123)), ModelFixtures.trackItem(Urn.forTrack(456)));

        when(syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_TRACKS)).thenReturn(Observable.just(Result.SYNCED));
        when(recommendationsStorage.allSeeds()).thenReturn(Observable.just(recommendationSeed));
        when(recommendationsStorage.recommendedTracksForSeed(seedId)).thenReturn(Observable.just(recommendedTracksUrns));
        when(trackRepository.trackListFromUrns(recommendedTracksUrns)).thenReturn(Single.just(recommendedTracks));

        OldDiscoveryItem oldDiscoveryItem = operations.allBuckets().test()
                                                      .assertValueCount(1)
                                                      .assertCompleted()
                                                      .getOnNextEvents().get(0);

        assertThat(((RecommendedTracksBucketItem) oldDiscoveryItem).seedTrackQueryPosition()).isEqualTo(0);
        assertThat(((RecommendedTracksBucketItem) oldDiscoveryItem).seedTrackUrn()).isEqualTo(seedTrackUrn);
    }

    @Test
    public void allBucketsRetainOrder() throws Exception {
        int firstSeedId = 1;
        Urn firstSeedTrackUrn = Urn.forTrack(2);
        RecommendationSeed firstRecommendationSeed = RecommendationSeed.create(firstSeedId, firstSeedTrackUrn, "First", RecommendationReason.LIKED, 0, Urn.NOT_SET);

        int secondSeedId = 2;
        Urn secondSeedTrackUrn = Urn.forTrack(3);
        RecommendationSeed secondRecommendationSeed = RecommendationSeed.create(secondSeedId, secondSeedTrackUrn, "Second", RecommendationReason.LIKED, 1, Urn.NOT_SET);
        List<Urn> recommendedTracksUrns = Arrays.asList(Urn.forTrack(123), Urn.forTrack(456));
        List<TrackItem> recommendedTracks = Arrays.asList(ModelFixtures.trackItem(Urn.forTrack(123)), ModelFixtures.trackItem(Urn.forTrack(456)));

        PublishSubject<Integer> firstStorageSubject = PublishSubject.create();
        PublishSubject<Integer> secondStorageSubject = PublishSubject.create();

        when(syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_TRACKS)).thenReturn(Observable.just(Result.SYNCED));
        when(recommendationsStorage.allSeeds()).thenReturn(Observable.just(firstRecommendationSeed, secondRecommendationSeed));
        when(recommendationsStorage.recommendedTracksForSeed(firstSeedId)).thenReturn(firstStorageSubject.map(tick -> recommendedTracksUrns));
        when(recommendationsStorage.recommendedTracksForSeed(secondSeedId)).thenReturn(secondStorageSubject.map(tick -> recommendedTracksUrns));
        when(trackRepository.trackListFromUrns(recommendedTracksUrns)).thenReturn(Single.just(recommendedTracks));


        TestSubscriber<OldDiscoveryItem> testSubscriber = TestSubscriber.create();
        operations.allBuckets().subscribe(testSubscriber);

        secondStorageSubject.onNext(2);
        secondStorageSubject.onCompleted();
        firstStorageSubject.onNext(1);
        firstStorageSubject.onCompleted();

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertValueCount(2);

        List<OldDiscoveryItem> items = testSubscriber.getOnNextEvents();
        assertThat(((RecommendedTracksBucketItem) items.get(0)).seedTrackQueryPosition()).isEqualTo(0);
        assertThat(((RecommendedTracksBucketItem) items.get(1)).seedTrackQueryPosition()).isEqualTo(1);
    }

    private void assertRecommendedTrackItem(OldDiscoveryItem oldDiscoveryItem) {
        assertThat(oldDiscoveryItem.getKind()).isEqualTo(OldDiscoveryItem.Kind.RecommendedTracksItem);

        final RecommendedTracksBucketItem recommendationBucket = (RecommendedTracksBucketItem) oldDiscoveryItem;
        assertThat(recommendationBucket.seedTrackLocalId()).isEqualTo(SEED_ID);
        assertThat(recommendationBucket.seedTrackUrn()).isEqualTo(SEED_TRACK.getUrn());
        assertThat(recommendationBucket.seedTrackTitle()).isEqualTo(SEED_TRACK.getTitle());
        assertThat(recommendationBucket.recommendationReason()).isEqualTo(REASON);
        assertThat(recommendationBucket.seedTrackQueryPosition()).isEqualTo(QUERY_POSITION);
        assertThat(recommendationBucket.queryUrn()).isEqualTo(QUERY_URN);

        assertThat(recommendationBucket.recommendations()
                                       .get(0)
                                       .getTrack().title()).isEqualTo(RECOMMENDED_TRACK.title());
        assertThat(recommendationBucket.recommendations().get(0).getTrack().creatorName()).isEqualTo(
                RECOMMENDED_TRACK.creatorName());
    }

    private RecommendationSeed createSeed() {
        return RecommendationSeed.create(
                SEED_ID,
                SEED_TRACK.getUrn(),
                SEED_TRACK.getTitle(),
                REASON,
                QUERY_POSITION,
                QUERY_URN
        );
    }

}
