package com.soundcloud.android.discovery;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.discovery.recommendations.RecommendationBucketRendererFactory;
import com.soundcloud.android.discovery.recommendations.TrackRecommendationListener;
import com.soundcloud.android.discovery.recommendations.TrackRecommendationPlaybackInitiator;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.stations.StartStationHandler;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.tracks.UpdatePlayableAdapterSubscriberFactory;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.RecyclerViewParallaxer;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.subscriptions.CompositeSubscription;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

class DiscoveryPresenter extends RecyclerViewPresenter<List<DiscoveryItem>, DiscoveryItem>
        implements DiscoveryAdapter.DiscoveryItemListenerBucket, TrackRecommendationListener {

    private final DiscoveryModulesProvider discoveryModulesProvider;
    private final UpdatePlayableAdapterSubscriberFactory updatePlayableAdapterSubscriberFactory;
    private final TrackRecommendationPlaybackInitiator trackRecommendationPlaybackInitiator;
    private final DiscoveryAdapter adapter;
    private final ImagePauseOnScrollListener imagePauseOnScrollListener;
    private final Navigator navigator;
    private final EventBus eventBus;
    private final StartStationHandler startStationPresenter;

    private CompositeSubscription subscription = new CompositeSubscription();

    @Inject
    DiscoveryPresenter(DiscoveryModulesProvider discoveryModulesProvider,
                       SwipeRefreshAttacher swipeRefreshAttacher,
                       DiscoveryAdapterFactory adapterFactory,
                       RecommendationBucketRendererFactory recommendationBucketRendererFactory,
                       ImagePauseOnScrollListener imagePauseOnScrollListener,
                       Navigator navigator,
                       EventBus eventBus,
                       StartStationHandler startStationPresenter,
                       TrackRecommendationPlaybackInitiator trackRecommendationPlaybackInitiator,
                       UpdatePlayableAdapterSubscriberFactory updatePlayableAdapterSubscriberFactory) {
        super(swipeRefreshAttacher, Options.defaults());
        this.discoveryModulesProvider = discoveryModulesProvider;
        this.updatePlayableAdapterSubscriberFactory = updatePlayableAdapterSubscriberFactory;
        this.adapter = adapterFactory.create(recommendationBucketRendererFactory.create(true, this));
        this.imagePauseOnScrollListener = imagePauseOnScrollListener;
        this.navigator = navigator;
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
                eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                                   updatePlayableAdapterSubscriberFactory.create(adapter))
        );
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        getRecyclerView().removeOnScrollListener(imagePauseOnScrollListener);
        imagePauseOnScrollListener.resume();
        adapter.detach();
        super.onDestroyView(fragment);
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
        final Observable<List<DiscoveryItem>> source = discoveryModulesProvider
                .discoveryItems()
                .doOnCompleted(this::subscribeToUpdates);

        return CollectionBinding
                .from(source)
                .withAdapter(adapter).build();
    }


    @Override
    protected CollectionBinding<List<DiscoveryItem>, DiscoveryItem> onRefreshBinding() {
        adapter.setDiscoveryListener(this);
        final Observable<List<DiscoveryItem>> source = discoveryModulesProvider
                .refreshItems()
                .doOnCompleted(this::subscribeToUpdates);

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

    @Override
    public void dismissWelcomeUserItem(int position) {
        removeItem(position);
    }

    private void removeItem(int position) {
        adapter.removeItem(position);
        adapter.notifyItemRemoved(position);
    }
}
