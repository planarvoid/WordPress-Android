package com.soundcloud.android.discovery;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.stations.StartStationPresenter;
import com.soundcloud.android.stations.StationFixtures;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import java.util.Arrays;
import java.util.List;

public class DiscoveryPresenterTest extends AndroidUnitTest {
    public static final Urn TRACK_URN = Urn.forTrack(123L);
    @Mock private Fragment fragment;
    @Mock private Bundle bundle;

    private static final Urn SEED_TRACK_URN = Urn.forTrack(123L);
    private static final TrackItem RECOMMENDED_TRACK = TrackItem.from(ModelFixtures.create(ApiTrack.class));
    private static final Recommendation RECOMMENDATION = new Recommendation(RECOMMENDED_TRACK, SEED_TRACK_URN, false);
    private static final StationRecord STATION = StationFixtures.getStation(Urn.forTrackStation(123));
    private static final List<Urn> TRACKLIST = Arrays.asList(SEED_TRACK_URN, RECOMMENDED_TRACK.getUrn());

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private DiscoveryOperations discoveryOperations;
    @Mock private DiscoveryAdapterFactory adapterFactory;
    @Mock private DiscoveryAdapter adapter;
    @Mock private RecommendationBucketRendererFactory recommendationBucketRendererFactory;
    @Mock private RecommendationBucketRenderer recommendationBucketRenderer;
    @Mock private ImagePauseOnScrollListener imagePauseOnScrollListener;
    @Mock private Navigator navigator;
    @Mock private FeatureFlags featureFlags;
    @Mock private ChartsPresenter chartsPresenter;
    @Mock private StartStationPresenter startStationPresenter;

    private EventBus eventBus = new TestEventBus();
    private DiscoveryPresenter presenter;

    @Before
    public void setUp() {
        when(featureFlags.isEnabled(Flag.DISCOVERY_RECOMMENDATIONS)).thenReturn(true);
        when(recommendationBucketRendererFactory.create(any(Screen.class), any(Boolean.class))).thenReturn(recommendationBucketRenderer);
        when(adapterFactory.create(any(RecommendationBucketRenderer.class))).thenReturn(adapter);
        when(discoveryOperations.discoveryItems()).thenReturn(Observable.<List<DiscoveryItem>>empty());

        this.presenter = new DiscoveryPresenter(
                swipeRefreshAttacher,
                discoveryOperations,
                adapterFactory,
                recommendationBucketRendererFactory,
                imagePauseOnScrollListener,
                navigator,
                featureFlags,
                eventBus,
                startStationPresenter);

        presenter.onCreate(fragment, bundle);
    }

    @After
    public void tearDown() throws Exception {
        presenter.onDestroy(fragment);
    }

    @Test
    public void clickOnRecommendedStationStartsPlayingStation() {
        presenter.onRecommendedStationClicked(context(), STATION);

        verify(startStationPresenter).startStation(context(), STATION.getUrn());
    }

    @Test
    public void tagSelectedOpensPlaylistDiscoveryActivity() {
        final String playListTag = "playListTag";
        final Context context = context();

        presenter.onTagSelected(context, playListTag);

        verify(navigator).openPlaylistDiscoveryTag(context, playListTag);
    }

    @Test
    public void shouldUpdateAdapterOnTrackChange() {
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(
                                 TestPlayQueueItem.createTrack(TRACK_URN),
                                 Urn.NOT_SET,
                                 1));

        verify(adapter).updateNowPlaying(TRACK_URN);
    }

    @Test
    public void shouldNotUpdateAdapterOnTrackChangeAfterViewDestroyed() {
        presenter.onDestroy(fragment);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(
                                 TestPlayQueueItem.createTrack(TRACK_URN),
                                 Urn.NOT_SET,
                                 1));

        verify(adapter, never()).updateNowPlaying(TRACK_URN);
    }
}
