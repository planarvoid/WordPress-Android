package com.soundcloud.android.olddiscovery;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.R;
import com.soundcloud.android.olddiscovery.perf.DiscoveryMeasurements;
import com.soundcloud.android.olddiscovery.perf.DiscoveryMeasurementsFactory;
import com.soundcloud.android.olddiscovery.recommendations.RecommendationBucketRenderer;
import com.soundcloud.android.olddiscovery.recommendations.RecommendationBucketRendererFactory;
import com.soundcloud.android.olddiscovery.recommendations.TrackRecommendationPlaybackInitiator;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.stations.StartStationHandler;
import com.soundcloud.android.stations.StationFixtures;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.RefreshableFragmentRule;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.tracks.UpdatePlayableAdapterSubscriber;
import com.soundcloud.android.tracks.UpdatePlayableAdapterSubscriberFactory;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import rx.Observable;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.util.Collections;
import java.util.List;

public class OldDiscoveryPresenterTest extends AndroidUnitTest {

    @Rule public final FragmentRule fragmentRule = new RefreshableFragmentRule(R.layout.default_recyclerview_with_refresh);

    private static final Urn SEED_URN = new Urn("soundcloud:tracks:seed");
    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final StationRecord STATION = StationFixtures.getStation(Urn.forTrackStation(123));

    @Mock private Fragment fragment;
    @Mock private Bundle bundle;
    @Mock private OldDiscoveryModulesProvider oldDiscoveryModulesProvider;
    @Mock private OldDiscoveryAdapterFactory adapterFactory;
    @Mock private OldDiscoveryAdapter adapter;
    @Mock private RecommendationBucketRendererFactory recommendationBucketRendererFactory;
    @Mock private RecommendationBucketRenderer recommendationBucketRenderer;
    @Mock private ImagePauseOnScrollListener imagePauseOnScrollListener;
    @Mock private NavigationExecutor navigationExecutor;
    @Mock private StartStationHandler startStationHandler;
    @Mock private TrackRecommendationPlaybackInitiator trackRecommendationPlaybackInitiator;
    @Mock private List<OldDiscoveryItem> oldDiscoveryItems;
    @Mock private UpdatePlayableAdapterSubscriberFactory updatePlayableAdapterSubscriberFactory;
    @Mock private OldDiscoveryOperations oldDiscoveryOperations;
    @Mock private DiscoveryMeasurementsFactory discoveryMeasurementsFactory;
    @Mock private DiscoveryMeasurements discoveryMeasurements;

    @Spy private SwipeRefreshAttacherStub swipeRefreshAttacher;

    private UpdatePlayableAdapterSubscriber updatePlayableAdapterSubscriber;
    private TestEventBus eventBus = new TestEventBus();
    private OldDiscoveryPresenter presenter;

    @Before
    public void setUp() {
        when(adapterFactory.create(recommendationBucketRenderer)).thenReturn(adapter);
        when(adapter.getItems()).thenReturn(oldDiscoveryItems);
        when(oldDiscoveryModulesProvider.discoveryItems()).thenReturn(Observable.empty());
        when(recommendationBucketRendererFactory
                     .create(eq(true), any(OldDiscoveryPresenter.class))).thenReturn(recommendationBucketRenderer);
        updatePlayableAdapterSubscriber = spy(new UpdatePlayableAdapterSubscriber(adapter));
        when(updatePlayableAdapterSubscriberFactory.create(adapter)).thenReturn(updatePlayableAdapterSubscriber);
        when(discoveryMeasurementsFactory.create()).thenReturn(discoveryMeasurements);

        this.presenter = new OldDiscoveryPresenter(
                oldDiscoveryModulesProvider,
                swipeRefreshAttacher,
                adapterFactory,
                recommendationBucketRendererFactory,
                imagePauseOnScrollListener,
                navigationExecutor,
                eventBus,
                startStationHandler,
                trackRecommendationPlaybackInitiator,
                updatePlayableAdapterSubscriberFactory,
                oldDiscoveryOperations,
                discoveryMeasurementsFactory);

        presenter.onCreate(fragment, bundle);
    }

    @After
    public void tearDown() throws Exception {
        presenter.onDestroy(fragment);
    }

    @Test
    public void clickOnRecommendedStationStartsPlayingStation() {
        AppCompatActivity activity = activity();
        presenter.onRecommendedStationClicked(activity, STATION);

        verify(startStationHandler).startStation(STATION.getUrn(), DiscoverySource.STATIONS_SUGGESTIONS);
    }

    @Test
    public void tagSelectedOpensPlaylistDiscoveryActivity() {
        final String playListTag = "playListTag";
        final Context context = context();

        presenter.onTagSelected(context, playListTag);

        verify(navigationExecutor).openPlaylistDiscoveryTag(context, playListTag);
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
        verify(trackRecommendationPlaybackInitiator).playFromReason(SEED_URN, Screen.SEARCH_MAIN, oldDiscoveryItems);
    }

    @Test
    public void propagatesOnTrackClickedToRecommendationPlaybackInitiator() {
        presenter.onTrackClicked(SEED_URN, TRACK_URN);
        verify(trackRecommendationPlaybackInitiator).playFromRecommendation(SEED_URN,
                                                                            TRACK_URN,
                                                                            Screen.SEARCH_MAIN,
                                                                            oldDiscoveryItems);
    }

    @Test
    public void resumesImageLoadingOnViewDestroy() {
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onDestroyView(fragmentRule.getFragment());

        verify(imagePauseOnScrollListener).resume();
    }

    @Test
    public void dismissesUpsellItem() {
        presenter.onUpsellItemDismissed(0, upsellItem());

        verify(oldDiscoveryOperations).disableUpsell();
        verify(adapter).removeItem(0);
        verify(adapter).notifyItemRemoved(0);
    }

    @Test
    public void handlesUpsellItemClicked() {
        presenter.onUpsellItemClicked(context(), 0, upsellItem());

        verify(navigationExecutor).openUpgrade(context(), UpsellContext.PREMIUM_CONTENT);
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(UpgradeFunnelEvent.class);
    }

    @Test
    public void handlesUpsellItemCreated() {
        presenter.onUpsellItemCreated();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(UpgradeFunnelEvent.class);
    }

    @Test
    public void shouldEndMeasuringLoginPerformanceWhenDiscoveryIsHome() {
        List<OldDiscoveryItem> items = Collections.singletonList(OldDiscoveryItem.forSearchItem());
        when(oldDiscoveryModulesProvider.discoveryItems()).thenReturn(Observable.just(items));

        presenter.onCreate(fragmentRule.getFragment(), null);

        verify(discoveryMeasurements).endLoading();
    }

    @Test
    public void shouldInvokeDiscoveryMeasurementsOnRefresh() {
        List<OldDiscoveryItem> items = Collections.singletonList(OldDiscoveryItem.forSearchItem());
        when(oldDiscoveryModulesProvider.refreshItems()).thenReturn(Observable.just(items));

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        swipeRefreshAttacher.forceRefresh();

        InOrder inOrder = inOrder(discoveryMeasurements);
        inOrder.verify(discoveryMeasurements).startRefreshing();
        inOrder.verify(discoveryMeasurements).endRefreshing();
    }

    @NonNull
    private OldDiscoveryItem upsellItem() {
        return OldDiscoveryItem.Default.create(OldDiscoveryItem.Kind.UpsellItem);
    }

    private static class SwipeRefreshAttacherStub extends SwipeRefreshAttacher {

        private SwipeRefreshLayout.OnRefreshListener listener;

        @Override
        public void attach(SwipeRefreshLayout.OnRefreshListener listener,
                           MultiSwipeRefreshLayout swipeRefreshLayout,
                           View... refreshableChildren) {
            super.attach(listener, swipeRefreshLayout, refreshableChildren);
            this.listener = listener;
        }

        void forceRefresh() {
            listener.onRefresh();
        }
    }
}
