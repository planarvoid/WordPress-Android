package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.discovery.recommendations.RecommendationBucketRenderer;
import com.soundcloud.android.discovery.recommendations.RecommendationBucketRendererFactory;
import com.soundcloud.android.discovery.recommendations.TrackRecommendationPlaybackInitiator;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.stations.StartStationHandler;
import com.soundcloud.android.stations.StationFixtures;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.Assertions;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.tracks.UpdatePlayableAdapterSubscriber;
import com.soundcloud.android.tracks.UpdatePlayableAdapterSubscriberFactory;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import java.util.Collections;
import java.util.List;

public class DiscoveryPresenterTest extends AndroidUnitTest {

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh);

    private static final Urn SEED_URN = new Urn("soundcloud:tracks:seed");
    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final StationRecord STATION = StationFixtures.getStation(Urn.forTrackStation(123));

    @Mock private Fragment fragment;
    @Mock private Bundle bundle;
    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private DiscoveryModulesProvider discoveryModulesProvider;
    @Mock private DiscoveryAdapterFactory adapterFactory;
    @Mock private DiscoveryAdapter adapter;
    @Mock private RecommendationBucketRendererFactory recommendationBucketRendererFactory;
    @Mock private RecommendationBucketRenderer recommendationBucketRenderer;
    @Mock private ImagePauseOnScrollListener imagePauseOnScrollListener;
    @Mock private Navigator navigator;
    @Mock private StartStationHandler startStationHandler;
    @Mock private TrackRecommendationPlaybackInitiator trackRecommendationPlaybackInitiator;
    @Mock private List<DiscoveryItem> discoveryItems;
    @Mock private UpdatePlayableAdapterSubscriberFactory updatePlayableAdapterSubscriberFactory;
    @Mock private DiscoveryOperations discoveryOperations;
    @Mock private DefaultHomeScreenConfiguration defaultHomeScreenConfiguration;
    @Mock private PerformanceMetricsEngine performanceMetricsEngine;

    @Captor private ArgumentCaptor<PerformanceMetric> performanceMetricArgumentCaptor;

    private UpdatePlayableAdapterSubscriber updatePlayableAdapterSubscriber;
    private TestEventBus eventBus = new TestEventBus();
    private DiscoveryPresenter presenter;

    @Before
    public void setUp() {
        when(adapterFactory.create(recommendationBucketRenderer)).thenReturn(adapter);
        when(adapter.getItems()).thenReturn(discoveryItems);
        when(discoveryModulesProvider.discoveryItems()).thenReturn(Observable.empty());
        when(recommendationBucketRendererFactory
                     .create(eq(true), any(DiscoveryPresenter.class))).thenReturn(recommendationBucketRenderer);
        updatePlayableAdapterSubscriber = spy(new UpdatePlayableAdapterSubscriber(adapter));
        when(updatePlayableAdapterSubscriberFactory.create(adapter)).thenReturn(updatePlayableAdapterSubscriber);

        this.presenter = new DiscoveryPresenter(
                discoveryModulesProvider,
                swipeRefreshAttacher,
                adapterFactory,
                recommendationBucketRendererFactory,
                imagePauseOnScrollListener,
                navigator,
                eventBus,
                startStationHandler,
                trackRecommendationPlaybackInitiator,
                updatePlayableAdapterSubscriberFactory,
                discoveryOperations,
                defaultHomeScreenConfiguration,
                performanceMetricsEngine);

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
        final CurrentPlayQueueItemEvent event = CurrentPlayQueueItemEvent.fromPositionChanged(
                TestPlayQueueItem.createTrack(TRACK_URN),
                Urn.NOT_SET,
                1);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, event);

        verify(updatePlayableAdapterSubscriber).onNext(event);
    }

    @Test
    public void shouldNotUpdateAdapterOnTrackChangeAfterViewDestroyed() {
        presenter.onDestroy(fragment);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(
                                 TestPlayQueueItem.createTrack(TRACK_URN),
                                 Urn.NOT_SET,
                                 1));

        verify(updatePlayableAdapterSubscriber, never()).onNext(any(CurrentPlayQueueItemEvent.class));
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

    @Test
    public void resumesImageLoadingOnViewDestroy() {
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onDestroyView(fragmentRule.getFragment());

        verify(imagePauseOnScrollListener).resume();
    }

    @Test
    public void dismissesUpsellItem() {
        presenter.onUpsellItemDismissed(0);

        verify(discoveryOperations).disableUpsell();
        verify(adapter).removeItem(0);
        verify(adapter).notifyItemRemoved(0);
    }

    @Test
    public void handlesUpsellItemClicked() {
        presenter.onUpsellItemClicked(context(), 0);

        verify(navigator).openUpgrade(context());
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(UpgradeFunnelEvent.class);
    }

    @Test
    public void handlesUpsellItemCreated() {
        presenter.onUpsellItemCreated();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(UpgradeFunnelEvent.class);
    }

    @Test
    public void shouldEndMeasuringLoginPerformanceWhenDiscoveryIsHome() {
        List<DiscoveryItem> items = Collections.singletonList(DiscoveryItem.forSearchItem());
        when(discoveryModulesProvider.discoveryItems()).thenReturn(Observable.just(items));
        when(defaultHomeScreenConfiguration.isDiscoveryHome()).thenReturn(true);

        presenter.onCreate(fragmentRule.getFragment(), null);

        verify(performanceMetricsEngine).endMeasuring(performanceMetricArgumentCaptor.capture());

        Assertions.assertThat(performanceMetricArgumentCaptor.getValue())
                  .hasMetricType(MetricType.LOGIN)
                  .containsMetricParam(MetricKey.HOME_SCREEN, Screen.SEARCH_MAIN.get());
    }

    @Test
    public void shouldNotEndMeasuringLoginPerformanceWhenStreamIsNotHome() {
        List<DiscoveryItem> items = Collections.singletonList(DiscoveryItem.forSearchItem());
        when(discoveryModulesProvider.discoveryItems()).thenReturn(Observable.just(items));
        when(defaultHomeScreenConfiguration.isDiscoveryHome()).thenReturn(false);

        presenter.onCreate(fragmentRule.getFragment(), null);

        verify(performanceMetricsEngine, never()).endMeasuring(any(PerformanceMetric.class));
    }
}
