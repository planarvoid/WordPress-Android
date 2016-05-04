package com.soundcloud.android.discovery;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import android.content.Context;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DiscoveryPresenterTest extends AndroidUnitTest {

    private static final Urn SEED_TRACK_URN = Urn.forTrack(1L);
    private static final Urn RECOMMENDATION_URN = Urn.forTrack(2L);
    private static final Urn RECOMMENDED_TRACK_URN = Urn.forTrack(3L);

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private DiscoveryOperations discoveryOperations;
    @Mock private DiscoveryAdapter adapter;
    @Mock private ImagePauseOnScrollListener imagePauseOnScrollListener;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private Navigator navigator;
    @Mock private FeatureFlags featureFlags;

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh);

    private DiscoveryPresenter presenter;

    private TestSubscriber testSubscriber = new TestSubscriber();
    private Provider expandPlayerSubscriberProvider = providerOf(testSubscriber);

    @Mock RecommendationBucket recommendationBucketOne;
    @Mock RecommendationBucket recommendationBucketTwo;

    @Rule public ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        this.presenter = new DiscoveryPresenter(swipeRefreshAttacher, discoveryOperations,
                adapter, imagePauseOnScrollListener, expandPlayerSubscriberProvider, playbackInitiator,
                navigator, featureFlags);

        when(recommendationBucketOne.getSeedTrackUrn()).thenReturn(SEED_TRACK_URN);
        when(featureFlags.isEnabled(Flag.DISCOVERY_RECOMMENDATIONS)).thenReturn(true);
    }

    @Test
    public void clickOnRecommendationReasonPlaysSeedAndRecommendedTracks() {
        PublishSubject<List<DiscoveryItem>> discoveryItems = PublishSubject.create();
        when(discoveryOperations.discoveryItemsAndRecommendations()).thenReturn(discoveryItems);

        PublishSubject<List<Urn>> recommendedTracksForSeed = PublishSubject.create();
        when(discoveryOperations.recommendedTracksWithSeed(any(RecommendationBucket.class))).thenReturn(recommendedTracksForSeed);

        discoveryItems.onNext(Arrays.<DiscoveryItem>asList(recommendationBucketOne, recommendationBucketTwo));
        recommendedTracksForSeed.onNext(Arrays.asList(SEED_TRACK_URN, RECOMMENDED_TRACK_URN));

        when(playbackInitiator.playTracks(eq(recommendedTracksForSeed), eq(SEED_TRACK_URN), eq(0), isA(PlaySessionSource.class)))
                .thenReturn(Observable.just(PlaybackResult.success()));

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
    }

    @Test
    public void clickOnRecommendationArtworkPlaysRecommendedTracks() {
        PublishSubject<List<DiscoveryItem>> discoveryItems = PublishSubject.create();
        when(discoveryOperations.discoveryItemsAndRecommendations()).thenReturn(discoveryItems);

        PublishSubject<List<Urn>> recommendedTracks = PublishSubject.create();
        when(discoveryOperations.recommendedTracks()).thenReturn(recommendedTracks);

        discoveryItems.onNext(Arrays.<DiscoveryItem>asList(recommendationBucketOne, recommendationBucketTwo));
        recommendedTracks.onNext(Collections.singletonList(RECOMMENDED_TRACK_URN));

        when(playbackInitiator.playTracks(eq(recommendedTracks), eq(RECOMMENDATION_URN), eq(0), isA(PlaySessionSource.class)))
                .thenReturn(Observable.just(PlaybackResult.success()));

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
    }

    @Test
    public void tagSelectedOpensPlaylistDiscoveryActivity() {
        final String playListTag = "playListTag";
        final Context context = context();

        presenter.onTagSelected(context, playListTag);

        verify(navigator).openPlaylistDiscoveryTag(context, playListTag);
    }
}
