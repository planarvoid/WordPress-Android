package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.PlaylistDiscoveryOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DiscoveryOperationsTest extends AndroidUnitTest {

    private static final long SEED_ID = 1;
    private static final RecommendationReason REASON = RecommendationReason.LIKED;
    private static final List<String> POPULAR_TAGS = Arrays.asList("popTag1", "popTag2");
    private static final List<String> RECENT_TAGS = Arrays.asList("recentTag1", "recentTag2");

    private final Scheduler scheduler = Schedulers.immediate();
    private final TestSubscriber<List<DiscoveryItem>> observer = new TestSubscriber<>();
    private final PublishSubject<Boolean> syncSubject = PublishSubject.create();
    private final ApiTrack seedTrack = ModelFixtures.create(ApiTrack.class);
    private final ApiTrack recommendedTrack = ModelFixtures.create(ApiTrack.class);
    private final List<ApiTrack> recommendedTracks = ModelFixtures.create(ApiTrack.class, 2);

    private DiscoveryOperations operations;

    @Mock private RecommendationsSync recommendationsSync;
    @Mock private RecommendationsStorage recommendationsStorage;
    @Mock private PlaylistDiscoveryOperations playlistDiscoveryOperations;

    @Before
    public void setUp() throws Exception {
        operations = new DiscoveryOperations(recommendationsSync, recommendationsStorage,
                playlistDiscoveryOperations, scheduler);

        // setup happy path
        when(recommendationsStorage.seedTracks()).thenReturn(Observable.just(Arrays.asList(createSeedItem())));
        when(recommendationsSync.syncRecommendations()).thenReturn(syncSubject);
        when(playlistDiscoveryOperations.popularPlaylistTags()).thenReturn(Observable.just(POPULAR_TAGS));
        when(playlistDiscoveryOperations.recentPlaylistTags()).thenReturn(Observable.just(RECENT_TAGS));
    }

    @Test
    public void loadsRecommendationsFollowedByPlaylistDiscoTags() {
        operations.recommendationsAndPlaylistDiscovery().subscribe(observer);
        observer.assertNoValues(); // make sure we sync before loading
        syncSubject.onNext(true);

        final List<List<DiscoveryItem>> onNextEvents = observer.getOnNextEvents();
        assertThat(onNextEvents).hasSize(1);

        final List<DiscoveryItem> discoveryItems = onNextEvents.get(0);
        assertThat(discoveryItems).hasSize(2);

        assertRecommendedTrackItem(discoveryItems.get(0));
        assertPlaylistDiscoItem(discoveryItems.get(1), POPULAR_TAGS, RECENT_TAGS);
    }

    @Test
    public void loadsPlaylistDiscoTagsWhenRecommendationsSyncErrors() {
        when(recommendationsSync.syncRecommendations()).thenReturn(Observable.<Boolean>error(new IOException()));

        operations.recommendationsAndPlaylistDiscovery().subscribe(observer);

        final List<List<DiscoveryItem>> onNextEvents = observer.getOnNextEvents();
        assertThat(onNextEvents).hasSize(1);

        final List<DiscoveryItem> discoveryItems = onNextEvents.get(0);
        assertThat(discoveryItems).hasSize(1);

        assertPlaylistDiscoItem(discoveryItems.get(0), POPULAR_TAGS, RECENT_TAGS);
    }

    @Test
    public void loadsPlaylistDiscoTagsWhenRecommendationsLoadErrors() {
        when(recommendationsStorage.seedTracks()).thenReturn(Observable.<List<PropertySet>>error(new IOException()));

        operations.recommendationsAndPlaylistDiscovery().subscribe(observer);
        syncSubject.onNext(true);

        final List<List<DiscoveryItem>> onNextEvents = observer.getOnNextEvents();
        assertThat(onNextEvents).hasSize(1);

        final List<DiscoveryItem> discoveryItems = onNextEvents.get(0);
        assertThat(discoveryItems).hasSize(1);

        assertPlaylistDiscoItem(discoveryItems.get(0), POPULAR_TAGS, RECENT_TAGS);
    }

    @Test
    public void loadsRecommendationsWhenPlaylistRecentTagsLoadErrors() {
        when(playlistDiscoveryOperations.recentPlaylistTags()).thenReturn(Observable.<List<String>>error(new IOException()));

        operations.recommendationsAndPlaylistDiscovery().subscribe(observer);
        syncSubject.onNext(true);

        final List<List<DiscoveryItem>> onNextEvents = observer.getOnNextEvents();
        assertThat(onNextEvents).hasSize(1);

        final List<DiscoveryItem> discoveryItems = onNextEvents.get(0);
        assertThat(discoveryItems).hasSize(1);

        assertRecommendedTrackItem(discoveryItems.get(0));
    }

    @Test
    public void loadsRecommendationsWhenPlaylistPopularTagsLoadErrors() {
        when(playlistDiscoveryOperations.popularPlaylistTags()).thenReturn(Observable.<List<String>>error(new IOException()));

        operations.recommendationsAndPlaylistDiscovery().subscribe(observer);
        syncSubject.onNext(true);

        final List<List<DiscoveryItem>> onNextEvents = observer.getOnNextEvents();
        assertThat(onNextEvents).hasSize(1);

        final List<DiscoveryItem> discoveryItems = onNextEvents.get(0);
        assertThat(discoveryItems).hasSize(1);

        assertRecommendedTrackItem(discoveryItems.get(0));
    }

    @Test
    public void loadsAllRecommendedTracksWithSeed() {
        final TestSubscriber<List<Urn>> testSubscriber = new TestSubscriber<>();
        final Urn recommendedTrackUrnOne = Urn.forTrack(2L);
        final Urn recommendedTrackUrnTwo = Urn.forTrack(3L);

        when(recommendationsStorage.recommendedTracksBeforeSeed(SEED_ID)).thenReturn(Observable.just(Collections.singletonList(recommendedTrackUrnOne)));
        when(recommendationsStorage.recommendedTracksAfterSeed(SEED_ID)).thenReturn(Observable.just(Collections.singletonList(recommendedTrackUrnTwo)));

        RecommendationItem recommendationItem = new RecommendationItem(createSeedItem());
        operations.recommendedTracksWithSeed(recommendationItem).subscribe(testSubscriber);

        List<Urn> recommendedTracksWithSeed = testSubscriber.getOnNextEvents().get(0);

        assertThat(recommendedTracksWithSeed.get(0)).isEqualTo(recommendedTrackUrnOne);
        assertThat(recommendedTracksWithSeed.get(1)).isEqualTo(seedTrack.getUrn());
        assertThat(recommendedTracksWithSeed.get(2)).isEqualTo(recommendedTrackUrnTwo);
    }

    @Test
    public void loadsRecommendedTracksForSeed() {
        final TestSubscriber<List<RecommendedTrackItem>> testSubscriber = new TestSubscriber<>();

        when(recommendationsStorage.recommendedTracksForSeed(SEED_ID)).thenReturn(Observable.just(createRecommendedTrackPropertySet()));

        operations.recommendedTracksForSeed(SEED_ID).subscribe(testSubscriber);

        List<RecommendedTrackItem> recommendedTracksForSeed = testSubscriber.getOnNextEvents().get(0);
        RecommendedTrackItem recommendedTrackItem = recommendedTracksForSeed.get(0);

        assertThat(recommendedTrackItem.getEntityUrn()).isEqualTo(recommendedTrack.getUrn());
        assertThat(recommendedTrackItem.getTitle()).isEqualTo(recommendedTrack.getTitle());
        assertThat(recommendedTrackItem.getCreatorName()).isEqualTo(recommendedTrack.getUserName());
        assertThat(recommendedTrackItem.getDuration()).isEqualTo(recommendedTrack.getDuration());
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

    private void assertRecommendedTrackItem(DiscoveryItem discoveryItem) {
        assertThat(discoveryItem.getKind()).isEqualTo(DiscoveryItem.Kind.TrackRecommendationItem);

        final RecommendationItem recommendationItem = (RecommendationItem) discoveryItem;
        assertThat(recommendationItem.getSeedTrackLocalId()).isEqualTo(SEED_ID);
        assertThat(recommendationItem.getSeedTrackUrn()).isEqualTo(seedTrack.getUrn());
        assertThat(recommendationItem.getSeedTrackTitle()).isEqualTo(seedTrack.getTitle());
        assertThat(recommendationItem.getRecommendationReason()).isEqualTo(REASON);
        assertThat(recommendationItem.getRecommendationCount()).isEqualTo(recommendedTracks.size());
        assertThat(recommendationItem.getRecommendationTitle()).isEqualTo(recommendedTracks.get(0).getTitle());
        assertThat(recommendationItem.getRecommendationUrn()).isEqualTo(recommendedTracks.get(0).getUrn());
        assertThat(recommendationItem.getRecommendationUserName()).isEqualTo(recommendedTracks.get(0).getUserName());
    }

    private void assertPlaylistDiscoItem(DiscoveryItem discoveryItem, List<String> popularTags, List<String> recentTags) {
        assertThat(discoveryItem.getKind()).isEqualTo(DiscoveryItem.Kind.PlaylistTagsItem);

        final PlaylistDiscoveryItem playlistDiscoItem = (PlaylistDiscoveryItem) discoveryItem;
        assertThat(playlistDiscoItem.getPopularTags()).isEqualTo(popularTags);
        assertThat(playlistDiscoItem.getRecentTags()).isEqualTo(recentTags);
    }

    private PropertySet createSeedItem() {
        return PropertySet.from(
                RecommendationProperty.SEED_TRACK_LOCAL_ID.bind(SEED_ID),
                RecommendationProperty.SEED_TRACK_URN.bind(seedTrack.getUrn()),
                RecommendationProperty.SEED_TRACK_TITLE.bind(seedTrack.getTitle()),
                RecommendationProperty.RECOMMENDED_TRACKS_COUNT.bind(recommendedTracks.size()),
                RecommendationProperty.REASON.bind(REASON),
                RecommendedTrackProperty.URN.bind(recommendedTracks.get(0).getUrn()),
                RecommendedTrackProperty.TITLE.bind(recommendedTracks.get(0).getTitle()),
                RecommendedTrackProperty.USERNAME.bind(recommendedTracks.get(0).getUserName())
        );
    }

    private List<PropertySet> createRecommendedTrackPropertySet() {
        PropertySet trackPropertySet = PropertySet.from(
                RecommendedTrackProperty.SEED_SOUND_URN.bind(Urn.forTrack(SEED_ID)),
                PlayableProperty.URN.bind(Urn.forTrack(recommendedTrack.getId())),
                PlayableProperty.TITLE.bind(recommendedTrack.getTitle()),
                PlayableProperty.CREATOR_NAME.bind(recommendedTrack.getUserName()),
                PlayableProperty.DURATION.bind(recommendedTrack.getDuration()),
                TrackProperty.PLAY_COUNT.bind(recommendedTrack.getPlaybackCount()),
                PlayableProperty.LIKES_COUNT.bind(recommendedTrack.getLikesCount()),
                PlayableProperty.CREATED_AT.bind(recommendedTrack.getCreatedAt())
        );
        return Collections.singletonList(trackPropertySet);
    }
}