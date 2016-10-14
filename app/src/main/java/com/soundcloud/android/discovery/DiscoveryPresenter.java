package com.soundcloud.android.discovery;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.configuration.experiments.ChartsExperiment;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.search.PlaylistDiscoveryOperations;
import com.soundcloud.android.stations.RecommendedStationsOperations;
import com.soundcloud.android.stations.StartStationHandler;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.utils.EmptyThrowable;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.RecyclerViewParallaxer;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class DiscoveryPresenter extends RecyclerViewPresenter<List<DiscoveryItem>, DiscoveryItem>
        implements DiscoveryAdapter.DiscoveryItemListenerBucket, TrackRecommendationListener {

    private final DataSource dataSource;
    private final TrackRecommendationPlaybackInitiator trackRecommendationPlaybackInitiator;
    private final DiscoveryAdapter adapter;
    private final ImagePauseOnScrollListener imagePauseOnScrollListener;
    private final Navigator navigator;
    private final FeatureFlags featureFlags;
    private final EventBus eventBus;
    private final StartStationHandler startStationPresenter;

    private CompositeSubscription subscription = new CompositeSubscription();

    private final Action0 subscribeToUpdates = new Action0() {
        @Override
        public void call() {
            subscribeToUpdates();
        }
    };

    @Inject
    DiscoveryPresenter(DataSource dataSource,
                       SwipeRefreshAttacher swipeRefreshAttacher,
                       DiscoveryAdapterFactory adapterFactory,
                       RecommendationBucketRendererFactory recommendationBucketRendererFactory,
                       ImagePauseOnScrollListener imagePauseOnScrollListener,
                       Navigator navigator,
                       FeatureFlags featureFlags,
                       EventBus eventBus,
                       StartStationHandler startStationPresenter,
                       TrackRecommendationPlaybackInitiator trackRecommendationPlaybackInitiator) {
        super(swipeRefreshAttacher, Options.defaults());
        this.dataSource = dataSource;
        this.adapter = adapterFactory.create(recommendationBucketRendererFactory.create(true, this));
        this.imagePauseOnScrollListener = imagePauseOnScrollListener;
        this.navigator = navigator;
        this.featureFlags = featureFlags;
        this.eventBus = eventBus;
        this.startStationPresenter = startStationPresenter;
        this.trackRecommendationPlaybackInitiator = trackRecommendationPlaybackInitiator;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    private void subscribeToUpdates() {
        subscription.unsubscribe();
        subscription = new CompositeSubscription(
                eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM, new UpdatePlayingUrnSubscriber())
        );
    }

    @Override
    public void onDestroy(Fragment fragment) {
        subscription.unsubscribe();
        super.onDestroy(fragment);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        addScrollListeners();
    }

    @Override
    public void onSearchClicked(Context context) {
        navigator.openSearch((Activity) context);
    }

    @Override
    public void onTagSelected(Context context, String tag) {
        navigator.openPlaylistDiscoveryTag(context, tag);
    }

    @Override
    public void onReasonClicked(Urn seedUrn) {
        trackRecommendationPlaybackInitiator.playFromReason(seedUrn, Screen.SEARCH_MAIN, adapter.getItems());
    }

    @Override
    public void onTrackClicked(Urn seedUrn, Urn trackUrn) {
        trackRecommendationPlaybackInitiator.playFromRecommendation(seedUrn,
                                                                    trackUrn,
                                                                    Screen.SEARCH_MAIN,
                                                                    adapter.getItems());
    }

    @Override
    protected CollectionBinding<List<DiscoveryItem>, DiscoveryItem> onBuildBinding(Bundle bundle) {
        adapter.setDiscoveryListener(this);
        final Observable<List<DiscoveryItem>> source = dataSource
                .discoveryItems()
                .doOnCompleted(subscribeToUpdates);

        return CollectionBinding
                .from(source)
                .withAdapter(adapter).build();
    }


    @Override
    protected CollectionBinding<List<DiscoveryItem>, DiscoveryItem> onRefreshBinding() {
        adapter.setDiscoveryListener(this);
        final Observable<List<DiscoveryItem>> source = dataSource
                .refreshItems()
                .doOnCompleted(subscribeToUpdates);

        return CollectionBinding
                .from(source)
                .withAdapter(adapter).build();
    }

    private void addScrollListeners() {
        getRecyclerView().addOnScrollListener(imagePauseOnScrollListener);
        getRecyclerView().addOnScrollListener(new RecyclerViewParallaxer());
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    public void onRecommendedStationClicked(Context context, StationRecord station) {
        startStationPresenter.startStation(context, station.getUrn(), DiscoverySource.STATIONS_SUGGESTIONS);
    }

    private class UpdatePlayingUrnSubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {

        @Override
        public void onNext(CurrentPlayQueueItemEvent event) {
            if (adapter != null) {
                adapter.updateNowPlayingWithCollection(event.getCollectionUrn(),
                                                       event.getCurrentPlayQueueItem().getUrnOrNotSet());
            }
        }
    }

    public static class DataSource {
        private static final DiscoveryItem EMPTY_ITEM = EmptyViewItem.fromThrowable(new EmptyThrowable());
        private static final DiscoveryItem SEARCH_ITEM = DiscoveryItem.forSearchItem();

        private static final Func1<Throwable, DiscoveryItem> ERROR_ITEM = new Func1<Throwable, DiscoveryItem>() {
            @Override
            public DiscoveryItem call(Throwable throwable) {
                return EmptyViewItem.fromThrowable(throwable);
            }
        };

        private static final Func1<ChartBucket, DiscoveryItem> TO_DISCOVERY_ITEM = new Func1<ChartBucket, DiscoveryItem>() {
            @Override
            public DiscoveryItem call(ChartBucket chartBucket) {
                return ChartsBucketItem.from(chartBucket);
            }
        };

        private final RecommendedTracksOperations recommendedTracksOperations;
        private final PlaylistDiscoveryOperations playlistDiscoveryOperations;
        private final RecommendedStationsOperations recommendedStationsOperations;
        private final ChartsOperations chartsOperations;
        private final FeatureFlags featureFlags;
        private final ChartsExperiment chartsExperiment;

        @Inject
        public DataSource(RecommendedTracksOperations recommendedTracksOperations,
                          PlaylistDiscoveryOperations playlistDiscoveryOperations,
                          RecommendedStationsOperations recommendedStationsOperations,
                          ChartsOperations chartsOperations,
                          FeatureFlags featureFlags,
                          ChartsExperiment chartsExperiment) {
            this.recommendedTracksOperations = recommendedTracksOperations;
            this.playlistDiscoveryOperations = playlistDiscoveryOperations;
            this.recommendedStationsOperations = recommendedStationsOperations;
            this.chartsOperations = chartsOperations;
            this.featureFlags = featureFlags;
            this.chartsExperiment = chartsExperiment;
        }

        Observable<List<DiscoveryItem>> discoveryItems() {
            List<Observable<DiscoveryItem>> discoveryItems = new ArrayList<>(5);

            discoveryItems.add(recommendedStationsOperations.recommendedStations());
            discoveryItems.add(recommendedTracksOperations.recommendedTracks());

            if (featureFlags.isEnabled(Flag.DISCOVERY_CHARTS) || chartsExperiment.isEnabled()) {
                discoveryItems.add(chartsOperations.featuredCharts().map(TO_DISCOVERY_ITEM));
            }

            discoveryItems.add(playlistDiscoveryOperations.playlistTags());

            return items(discoveryItems);
        }

        Observable<List<DiscoveryItem>> refreshItems() {
            List<Observable<DiscoveryItem>> discoveryItems = new ArrayList<>(5);

            discoveryItems.add(recommendedStationsOperations.refreshRecommendedStations());
            discoveryItems.add(recommendedTracksOperations.refreshRecommendedTracks());

            if (featureFlags.isEnabled(Flag.DISCOVERY_CHARTS) || chartsExperiment.isEnabled()) {
                discoveryItems.add(chartsOperations.refreshFeaturedCharts().map(TO_DISCOVERY_ITEM));
            }

            discoveryItems.add(playlistDiscoveryOperations.playlistTags());

            return items(discoveryItems);
        }

        private Observable<List<DiscoveryItem>> items(List<Observable<DiscoveryItem>> discoveryItems) {
            return Observable
                    .just(discoveryItems)
                    .compose(RxUtils.<DiscoveryItem>concatEagerIgnorePartialErrors())
                    .defaultIfEmpty(EMPTY_ITEM)
                    .onErrorReturn(ERROR_ITEM)
                    .startWith(SEARCH_ITEM)
                    .toList();
        }

    }
}
