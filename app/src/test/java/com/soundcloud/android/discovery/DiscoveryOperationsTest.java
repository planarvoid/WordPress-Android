package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.PlaylistDiscoveryOperations;
import com.soundcloud.android.sync.recommendations.StoreRecommendationsCommand;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
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

public class DiscoveryOperationsTest extends AndroidUnitTest {

    private static final long SEED_ID = 1;
    private static final RecommendationReason REASON = RecommendationReason.LIKED;
    private static final List<String> POPULAR_TAGS = Arrays.asList("popTag1", "popTag2");
    private static final List<String> RECENT_TAGS = Arrays.asList("recentTag1", "recentTag2");

    private final Scheduler scheduler = Schedulers.immediate();
    private final TestSubscriber<List<DiscoveryItem>> subscriber = new TestSubscriber<>();
    private final PublishSubject<Boolean> syncSubject = PublishSubject.create();
    private final ApiTrack seedTrack = ModelFixtures.create(ApiTrack.class);
    private final List<ApiTrack> recommendedTracks = ModelFixtures.create(ApiTrack.class, 2);
    private final ApiTrack recommendedTrack = recommendedTracks.get(0);

    private DiscoveryOperations operations;

    @Mock private RecommendationsSyncInitiator recommendationsSyncInitiator;
    @Mock private RecommendationsStorage recommendationsStorage;
    @Mock private StoreRecommendationsCommand storeRecommendationsCommand;
    @Mock private PlaylistDiscoveryOperations playlistDiscoveryOperations;
    @Mock private RecommendedStationsOperations stationsOperations;

    @Before
    public void setUp() throws Exception {
        operations = new DiscoveryOperations(recommendationsSyncInitiator, recommendationsStorage,
                storeRecommendationsCommand, playlistDiscoveryOperations, stationsOperations, scheduler);

        // setup happy path
        when(recommendationsStorage.seedTracks()).thenReturn(Observable.just(Collections.singletonList(createSeed())));
        when(recommendationsStorage.firstSeed()).thenReturn(Observable.just(Optional.of(createSeed())));
        when(recommendationsStorage.recommendedTracksForSeed(SEED_ID)).thenReturn(Observable.just(createRecommendedTrackPropertySet()));
        when(recommendationsSyncInitiator.syncRecommendations()).thenReturn(syncSubject);
        when(playlistDiscoveryOperations.popularPlaylistTags()).thenReturn(Observable.just(POPULAR_TAGS));
        when(playlistDiscoveryOperations.recentPlaylistTags()).thenReturn(Observable.just(RECENT_TAGS));
        when(stationsOperations.getRecommendations()).thenReturn(Observable.<List<DiscoveryItem>>empty());
    }

    @Test
    public void loadsRecommendationsFollowedByPlaylistDiscoTags() {
        operations.discoveryItemsAndRecommendations().subscribe(subscriber);
        subscriber.assertValueCount(0); // prove that we wait for sync before fetching recommendations
        syncSubject.onNext(true);

        final List<List<DiscoveryItem>> onNextEvents = subscriber.getOnNextEvents();
        subscriber.assertValueCount(1);

        final List<DiscoveryItem> discoveryItems = onNextEvents.get(0);
        assertThat(discoveryItems).hasSize(2);

        assertRecommendedTrackItem(discoveryItems.get(0));
        assertPlaylistDiscoItem(discoveryItems.get(1), POPULAR_TAGS, RECENT_TAGS);
    }

    @Test
    public void loadsPlaylistDiscoTagsWhenRecommendationsSyncReturnsFailure() {
        when(recommendationsSyncInitiator.syncRecommendations()).thenReturn(Observable.just(false));

        operations.discoveryItemsAndRecommendations().subscribe(subscriber);

        final List<List<DiscoveryItem>> onNextEvents = subscriber.getOnNextEvents();
        subscriber.assertValueCount(1);

        final List<DiscoveryItem> discoveryItems = onNextEvents.get(0);
        assertThat(discoveryItems).hasSize(2);

        assertPlaylistDiscoItem(discoveryItems.get(1), POPULAR_TAGS, RECENT_TAGS);
    }

    @Test
    public void loadsPlaylistDiscoTagsWhenThereAreNoRecommendations() {
        when(recommendationsStorage.firstSeed()).thenReturn(Observable.just(Optional.<PropertySet>absent()));

        operations.discoveryItemsAndRecommendations().subscribe(subscriber);
        syncSubject.onNext(true);

        final List<List<DiscoveryItem>> onNextEvents = subscriber.getOnNextEvents();
        subscriber.assertValueCount(1);

        final List<DiscoveryItem> discoveryItems = onNextEvents.get(0);
        assertThat(discoveryItems).hasSize(1);

        assertPlaylistDiscoItem(discoveryItems.get(0), POPULAR_TAGS, RECENT_TAGS);
    }

    @Test
    public void loadsSearchItem() {
        operations.searchItem().subscribe(subscriber);

        final List<List<DiscoveryItem>> onNextEvents = subscriber.getOnNextEvents();
        assertThat(onNextEvents.size()).isEqualTo(1);
        assertSearchItem(onNextEvents.get(0).get(0));
    }

    @Test
    public void loadsRecommendationsAndPopularTagsWhenPlaylistRecentTagsIsEmpty() {
        when(playlistDiscoveryOperations.recentPlaylistTags()).thenReturn(Observable.just(Collections.<String>emptyList()));

        operations.discoveryItemsAndRecommendations().subscribe(subscriber);
        syncSubject.onNext(true);

        final List<List<DiscoveryItem>> onNextEvents = subscriber.getOnNextEvents();
        subscriber.assertValueCount(1);

        final List<DiscoveryItem> discoveryItems = onNextEvents.get(0);
        assertThat(discoveryItems).hasSize(2);

        assertRecommendedTrackItem(discoveryItems.get(0));
        assertPlaylistDiscoItem(discoveryItems.get(1), POPULAR_TAGS, Collections.<String>emptyList());
    }

    @Test
    public void loadsRecommendationsAndRecentTagsWhenPlaylistPopularTagsIsEmpty() {
        when(playlistDiscoveryOperations.popularPlaylistTags()).thenReturn(Observable.just(Collections.<String>emptyList()));

        operations.discoveryItemsAndRecommendations().subscribe(subscriber);
        syncSubject.onNext(true);

        final List<List<DiscoveryItem>> onNextEvents = subscriber.getOnNextEvents();
        subscriber.assertValueCount(1);

        final List<DiscoveryItem> discoveryItems = onNextEvents.get(0);
        assertThat(discoveryItems).hasSize(2);

        assertRecommendedTrackItem(discoveryItems.get(0));
        assertPlaylistDiscoItem(discoveryItems.get(1), Collections.<String>emptyList(), RECENT_TAGS);
    }

    @Test
    public void loadsAllRecommendedTracksWithSeed() {
        final TestSubscriber<List<Urn>> testSubscriber = new TestSubscriber<>();
        final Urn recommendedTrackUrnOne = Urn.forTrack(2L);
        final Urn recommendedTrackUrnTwo = Urn.forTrack(3L);

        when(recommendationsStorage.recommendedTracksBeforeSeed(SEED_ID)).thenReturn(Observable.just(Collections.singletonList(recommendedTrackUrnOne)));
        when(recommendationsStorage.recommendedTracksAfterSeed(SEED_ID)).thenReturn(Observable.just(Collections.singletonList(recommendedTrackUrnTwo)));

        RecommendationBucket recommendationBucket = new RecommendationBucket(createSeed(), Collections.<TrackItem>emptyList());
        operations.recommendedTracksWithSeed(recommendationBucket).subscribe(testSubscriber);

        List<Urn> recommendedTracksWithSeed = testSubscriber.getOnNextEvents().get(0);

        assertThat(recommendedTracksWithSeed.get(0)).isEqualTo(recommendedTrackUrnOne);
        assertThat(recommendedTracksWithSeed.get(1)).isEqualTo(seedTrack.getUrn());
        assertThat(recommendedTracksWithSeed.get(2)).isEqualTo(recommendedTrackUrnTwo);
    }

    @Test
    public void loadsRecommendedTracksForSeed() {
        final TestSubscriber<List<TrackItem>> testSubscriber = new TestSubscriber<>();

        when(recommendationsStorage.recommendedTracksForSeed(SEED_ID)).thenReturn(Observable.just(createRecommendedTrackPropertySet()));

        operations.recommendedTracksForSeed(SEED_ID).subscribe(testSubscriber);

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

        when(recommendationsStorage.recommendedTracks()).thenReturn(Observable.just(Arrays.asList(recommendedTrackUrnOne, recommendedTrackUrnTwo)));

        operations.recommendedTracks().subscribe(testObserver);

        List<Urn> recommendedTracks = testObserver.getOnNextEvents().get(0);

        assertThat(recommendedTracks.size()).isEqualTo(2);
        assertThat(recommendedTracks.contains(recommendedTrackUrnOne));
        assertThat(recommendedTracks.contains(recommendedTrackUrnTwo));
    }

    @Test
    public void loadsRecommendationsFromStorageDespiteSyncFailure() {
        when(recommendationsSyncInitiator.syncRecommendations()).thenReturn(Observable.just(false));

        operations.discoveryItemsAndRecommendations().subscribe(subscriber);

        final List<List<DiscoveryItem>> onNextEvents = subscriber.getOnNextEvents();
        subscriber.assertValueCount(1);

        final List<DiscoveryItem> discoveryItems = onNextEvents.get(0);
        assertThat(discoveryItems).hasSize(2);

        assertRecommendedTrackItem(discoveryItems.get(0));
    }

    @Test
    public void cleanUpRecommendationsData() {
        operations.clearData();

        verify(storeRecommendationsCommand).clearTables();
        verify(recommendationsSyncInitiator).clearLastSyncTime();
        verify(playlistDiscoveryOperations).clearData();
    }

    private void assertSearchItem(DiscoveryItem discoveryItem) {
        assertThat(discoveryItem.getKind()).isEqualTo(DiscoveryItem.Kind.SearchItem);
    }

    private void assertRecommendedTrackItem(DiscoveryItem discoveryItem) {
        assertThat(discoveryItem.getKind()).isEqualTo(DiscoveryItem.Kind.TrackRecommendationItem);

        final RecommendationBucket recommendationBucket = (RecommendationBucket) discoveryItem;
        assertThat(recommendationBucket.getSeedTrackLocalId()).isEqualTo(SEED_ID);
        assertThat(recommendationBucket.getSeedTrackUrn()).isEqualTo(seedTrack.getUrn());
        assertThat(recommendationBucket.getSeedTrackTitle()).isEqualTo(seedTrack.getTitle());
        assertThat(recommendationBucket.getRecommendationReason()).isEqualTo(REASON);

        assertThat(recommendationBucket.getRecommendations().get(0).getTitle()).isEqualTo(recommendedTracks.get(0).getTitle());
        assertThat(recommendationBucket.getRecommendations().get(0).getCreatorName()).isEqualTo(recommendedTracks.get(0).getUserName());
    }

    private void assertPlaylistDiscoItem(DiscoveryItem discoveryItem, List<String> popularTags, List<String> recentTags) {
        assertThat(discoveryItem.getKind()).isEqualTo(DiscoveryItem.Kind.PlaylistTagsItem);

        final PlaylistDiscoveryItem playlistDiscoItem = (PlaylistDiscoveryItem) discoveryItem;
        assertThat(playlistDiscoItem.getPopularTags()).isEqualTo(popularTags);
        assertThat(playlistDiscoItem.getRecentTags()).isEqualTo(recentTags);
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
