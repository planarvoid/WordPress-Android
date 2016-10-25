package com.soundcloud.android.discovery;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.discovery.recommendations.RecommendationBucketRenderer;
import com.soundcloud.android.discovery.recommendations.RecommendationBucketRendererFactory;
import com.soundcloud.android.discovery.recommendations.TrackRecommendationPlaybackInitiator;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.stations.StartStationHandler;
import com.soundcloud.android.stations.StationFixtures;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
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

import java.util.List;

public class DiscoveryPresenterTest extends AndroidUnitTest {

    private static final Urn SEED_URN = new Urn("soundcloud:tracks:seed");
    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final StationRecord STATION = StationFixtures.getStation(Urn.forTrackStation(123));

    @Mock private Fragment fragment;
    @Mock private Bundle bundle;
    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private DiscoveryPresenter.DataSource dataSource;
    @Mock private DiscoveryAdapterFactory adapterFactory;
    @Mock private DiscoveryAdapter adapter;
    @Mock private RecommendationBucketRendererFactory recommendationBucketRendererFactory;
    @Mock private RecommendationBucketRenderer recommendationBucketRenderer;
    @Mock private ImagePauseOnScrollListener imagePauseOnScrollListener;
    @Mock private Navigator navigator;
    @Mock private FeatureFlags featureFlags;
    @Mock private StartStationHandler startStationHandler;
    @Mock private TrackRecommendationPlaybackInitiator trackRecommendationPlaybackInitiator;
    @Mock private List<DiscoveryItem> discoveryItems;

    private EventBus eventBus = new TestEventBus();
    private DiscoveryPresenter presenter;

    @Before
    public void setUp() {
        when(adapterFactory.create(recommendationBucketRenderer)).thenReturn(adapter);
        when(adapter.getItems()).thenReturn(discoveryItems);
        when(dataSource.discoveryItems()).thenReturn(Observable.<List<DiscoveryItem>>empty());
        when(recommendationBucketRendererFactory
                     .create(eq(true), any(DiscoveryPresenter.class))).thenReturn(recommendationBucketRenderer);

        this.presenter = new DiscoveryPresenter(
                dataSource,
                swipeRefreshAttacher,
                adapterFactory,
                recommendationBucketRendererFactory,
                imagePauseOnScrollListener,
                navigator,
                eventBus,
                startStationHandler,
                trackRecommendationPlaybackInitiator);

        presenter.onCreate(fragment, bundle);
    }

    @After
    public void tearDown() throws Exception {
        presenter.onDestroy(fragment);
    }

    @Test
    public void clickOnRecommendedStationStartsPlayingStation() {
        presenter.onRecommendedStationClicked(context(), STATION);

        verify(startStationHandler).startStation(context(), STATION.getUrn(), DiscoverySource.STATIONS_SUGGESTIONS);
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

        verify(adapter).updateNowPlayingWithCollection(Urn.NOT_SET, TRACK_URN);
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

    @Test
    public void propagatesOnReasonClickedToRecommendationPlaybackInitiator() {
        presenter.onReasonClicked(SEED_URN);
        verify(trackRecommendationPlaybackInitiator).playFromReason(SEED_URN, Screen.SEARCH_MAIN, discoveryItems);
    }

    @Test
    public void propagatesOnTrackClickedToRecommendationPlaybackInitiator() {
        presenter.onTrackClicked(SEED_URN, TRACK_URN);
        verify(trackRecommendationPlaybackInitiator).playFromRecommendation(SEED_URN,
                                                                            TRACK_URN,
                                                                            Screen.SEARCH_MAIN,
                                                                            discoveryItems);
    }
}
