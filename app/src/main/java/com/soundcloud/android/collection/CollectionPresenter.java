package com.soundcloud.android.collection;

import com.soundcloud.android.configuration.experiments.GoOnboardingTooltipExperiment;
import com.soundcloud.android.events.GoOnboardingTooltipEvent;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.collection.CollectionItem.OnboardingCollectionItem;
import com.soundcloud.android.collection.CollectionItem.UpsellCollectionItem;
import com.soundcloud.android.collection.playhistory.PlayHistoryBucketItem;
import com.soundcloud.android.collection.playhistory.PlayHistoryOperations;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedBucketItem;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedPlayableItem;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperties;
import com.soundcloud.android.offline.OfflinePropertiesProvider;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.playback.ExpandPlayerObserver;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.OfflineItem;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.rx.observers.LambdaObserver;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

class CollectionPresenter extends RecyclerViewPresenter<MyCollection, CollectionItem>
        implements TrackItemRenderer.Listener, OnboardingItemCellRenderer.Listener, OfflineOnboardingItemCellRenderer.Listener, UpsellItemCellRenderer.Listener {

    private static final int FIXED_ITEMS = 5;

    @VisibleForTesting final Function<MyCollection, Iterable<CollectionItem>> toCollectionItems =
            new Function<MyCollection, Iterable<CollectionItem>>() {
                @Override
                public List<CollectionItem> apply(MyCollection myCollection) {
                    List<CollectionItem> collectionItems = buildCollectionItems(myCollection);
                    if (goOnboardingTooltipExperiment.isEnabled()
                            && featureOperations.isOfflineContentEnabled()
                            && collectionOptionsStorage.isOfflineOnboardingEnabled()) {
                        return collectionWithOfflineOnboarding(collectionItems);
                    } else if (collectionOptionsStorage.isOnboardingEnabled()) {
                        return collectionWithOnboarding(collectionItems);
                    } else if (!featureOperations.isOfflineContentEnabled()
                            && featureOperations.upsellBothTiers()
                            && collectionOptionsStorage.isUpsellEnabled()) {
                        return collectionWithUpsell(collectionItems);
                    } else {
                        return collectionItems;
                    }
                }
            };

    private final SwipeRefreshAttacher swipeRefreshAttacher;
    private final EventBusV2 eventBus;
    private final CollectionAdapter adapter;
    private final Resources resources;
    private final CollectionOptionsStorage collectionOptionsStorage;
    private final FeatureOperations featureOperations;
    private final NavigationExecutor navigationExecutor;
    private final OfflinePropertiesProvider offlinePropertiesProvider;
    private final FeatureFlags featureFlags;
    private final PerformanceMetricsEngine performanceMetricsEngine;
    private final CollectionOperations collectionOperations;
    private final Provider<ExpandPlayerObserver> expandPlayerObserverProvider;
    private final PlayHistoryOperations playHistoryOperations;
    private final GoOnboardingTooltipExperiment goOnboardingTooltipExperiment;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Inject
    CollectionPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                        CollectionOperations collectionOperations,
                        CollectionOptionsStorage collectionOptionsStorage,
                        CollectionAdapter adapter,
                        Resources resources,
                        EventBusV2 eventBus,
                        Provider<ExpandPlayerObserver> expandPlayerObserverProvider,
                        PlayHistoryOperations playHistoryOperations,
                        FeatureOperations featureOperations,
                        NavigationExecutor navigationExecutor,
                        OfflinePropertiesProvider offlinePropertiesProvider,
                        FeatureFlags featureFlags,
                        PerformanceMetricsEngine performanceMetricsEngine,
                        GoOnboardingTooltipExperiment goOnboardingTooltipExperiment) {
        super(swipeRefreshAttacher);
        this.collectionOperations = collectionOperations;
        this.expandPlayerObserverProvider = expandPlayerObserverProvider;
        this.playHistoryOperations = playHistoryOperations;
        this.swipeRefreshAttacher = swipeRefreshAttacher;
        this.eventBus = eventBus;
        this.adapter = adapter;
        this.resources = resources;
        this.collectionOptionsStorage = collectionOptionsStorage;
        this.featureOperations = featureOperations;
        this.navigationExecutor = navigationExecutor;
        this.offlinePropertiesProvider = offlinePropertiesProvider;
        this.featureFlags = featureFlags;
        this.performanceMetricsEngine = performanceMetricsEngine;
        this.goOnboardingTooltipExperiment = goOnboardingTooltipExperiment;

        adapter.setTrackClickListener(this);
        adapter.setOnboardingListener(this);
        adapter.setOfflineOnboardingListener(this);
        adapter.setUpsellListener(this);
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        final int itemMargin = view.getResources().getDimensionPixelSize(R.dimen.collection_default_margin);
        final int spanCount = resources.getInteger(R.integer.collection_grid_span_count);
        final GridLayoutManager layoutManager = new GridLayoutManager(view.getContext(), spanCount);
        layoutManager.setSpanSizeLookup(createSpanSizeLookup(spanCount));

        RecyclerView recyclerView = getRecyclerView();
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new CollectionItemDecoration(itemMargin));
        recyclerView.setPadding(itemMargin, 0, 0, 0);
        recyclerView.setClipToPadding(false);
        recyclerView.setClipChildren(false);

        // remove the blinking whenever we notifyItemChanged
        ((DefaultItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        adapter.detach();
        super.onDestroyView(fragment);
    }

    @Override
    public void onDestroy(Fragment fragment) {
        disposables.clear();
        super.onDestroy(fragment);
    }

    @Override
    protected CollectionBinding<MyCollection, CollectionItem> onBuildBinding(Bundle bundle) {
        final Observable<MyCollection> collections = collectionOperations.collections()
                                                                         .doOnSubscribe(disposable -> performanceMetricsEngine.startMeasuring(MetricType.COLLECTION_LOAD))
                                                                         .doOnComplete(() -> performanceMetricsEngine.endMeasuring(MetricType.COLLECTION_LOAD))
                                                                         .observeOn(AndroidSchedulers.mainThread());
        return CollectionBinding.fromV2(collections.doOnNext(new OnCollectionLoadedComsumer()), toCollectionItems)
                                .withAdapter(adapter)
                                .build();
    }

    @Override
    protected CollectionBinding<MyCollection, CollectionItem> onRefreshBinding() {
        final Observable<MyCollection> collections = collectionOperations.updatedCollections()
                                                                         .observeOn(AndroidSchedulers.mainThread());
        return CollectionBinding.fromV2(collections.doOnError(__ -> showError()), toCollectionItems)
                                .withAdapter(adapter)
                                .build();
    }

    @Override
    public void onCollectionsOnboardingItemClosed(int position) {
        collectionOptionsStorage.disableOnboarding();
        removeItem(position);
    }

    @Override
    public void onCollectionsOfflineOnboardingItemClosed(int position) {
        collectionOptionsStorage.disableOfflineOnboarding();
        removeItem(position);
    }

    @Override
    public void onUpsellClose(int position) {
        collectionOptionsStorage.disableUpsell();
        removeItem(position);
    }

    @Override
    public void onUpsell(Context context) {
        navigationExecutor.openUpgrade(context, UpsellContext.DEFAULT);
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forCollectionClick());
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    private void refreshCollections() {
        final Observable<MyCollection> source = collectionOperations.collections()
                                                                    .observeOn(AndroidSchedulers.mainThread());
        retryWith(CollectionBinding
                          .fromV2(source, toCollectionItems)
                          .withAdapter(adapter).build());
    }

    private GridLayoutManager.SpanSizeLookup createSpanSizeLookup(final int spanCount) {
        return new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position < adapter.getItemCount() && adapter.getItem(position).isSingleSpan()) {
                    return 1;
                } else {
                    return spanCount;
                }
            }
        };
    }

    private void removeItem(int position) {
        adapter.removeItem(position);
    }

    private void showError() {
        Toast.makeText(getRecyclerView().getContext(),
                       R.string.collections_loading_error,
                       Toast.LENGTH_LONG).show();
    }

    private void subscribeForUpdates() {
        disposables.clear();
        disposables.addAll(
                subscribeToOfflineContent(),
                collectionOperations.onCollectionChanged()
                                    .filter(event -> !swipeRefreshAttacher.isRefreshing())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribeWith(LambdaObserver.onNext(__ -> refreshCollections()))
        );
    }

    private Disposable subscribeToOfflineContent() {
        if (featureFlags.isEnabled(Flag.OFFLINE_PROPERTIES_PROVIDER)) {
            return offlinePropertiesProvider.statesV2()
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribeWith(new OfflinePropertiesSubscriber(adapter));
        } else {
            return eventBus.subscribe(EventQueue.OFFLINE_CONTENT_CHANGED, new UpdateCollectionDownloadSubscriber(adapter));
        }
    }

    private List<CollectionItem> collectionWithOnboarding(List<CollectionItem> collectionItems) {
        return prependItemToCollection(OnboardingCollectionItem.create(), collectionItems);
    }

    private List<CollectionItem> collectionWithOfflineOnboarding(List<CollectionItem> collectionItems) {
        eventBus.publish(EventQueue.TRACKING, GoOnboardingTooltipEvent.forCollectionImpression());
        return prependItemToCollection(CollectionItem.OfflineOnboardingCollectionItem.create(), collectionItems);
    }

    private List<CollectionItem> collectionWithUpsell(List<CollectionItem> collectionItems) {
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forCollectionImpression());
        return prependItemToCollection(UpsellCollectionItem.create(), collectionItems);
    }

    private List<CollectionItem> prependItemToCollection(CollectionItem item, List<CollectionItem> collectionItems) {
        List<CollectionItem> collection = new ArrayList<>(collectionItems.size() + 1);
        collection.add(item);
        collection.addAll(collectionItems);
        return collection;
    }

    private List<CollectionItem> buildCollectionItems(MyCollection myCollection) {
        List<TrackItem> playHistoryTrackItems = myCollection.getPlayHistoryTrackItems();
        List<RecentlyPlayedPlayableItem> recentlyPlayedPlayableItems = myCollection.getRecentlyPlayedItems();
        List<CollectionItem> collectionItems = new ArrayList<>(playHistoryTrackItems.size() + FIXED_ITEMS);

        collectionItems.add(PreviewCollectionItem.forLikesPlaylistsAndStations(myCollection.getLikes(),
                                                                               myCollection.getPlaylistAndAlbums(),
                                                                               myCollection.getPlaylists(),
                                                                               myCollection.getAlbums(),
                                                                               myCollection.getStations()));
        addRecentlyPlayed(recentlyPlayedPlayableItems, collectionItems);
        addPlayHistory(playHistoryTrackItems, collectionItems);

        return collectionItems;
    }

    private void addRecentlyPlayed(List<RecentlyPlayedPlayableItem> recentlyPlayedPlayableItems,
                                   List<CollectionItem> collectionItems) {
        collectionItems.add(RecentlyPlayedBucketItem.create(recentlyPlayedPlayableItems));
    }

    private void addPlayHistory(List<TrackItem> tracks, List<CollectionItem> collectionItems) {
        collectionItems.add(PlayHistoryBucketItem.create(tracks));
    }

    @Override
    public void trackItemClicked(Urn urn, int position) {
        disposables.add(playHistoryOperations.startPlaybackFrom(urn, Screen.COLLECTIONS).subscribeWith(expandPlayerObserverProvider.get()));
    }

    private class OnCollectionLoadedComsumer implements Consumer<MyCollection> {
        @Override
        public void accept(MyCollection myCollection) {
            subscribeForUpdates();
            if (myCollection.hasError()) {
                showError();
            }
        }
    }

    private static class OfflinePropertiesSubscriber extends DefaultObserver<OfflineProperties> {

        private final CollectionAdapter adapter;

        public OfflinePropertiesSubscriber(CollectionAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public void onNext(OfflineProperties states) {
            final List<CollectionItem> items = adapter.getItems();

            for (int position = 0; position < items.size(); position++) {
                final CollectionItem collectionItem = items.get(position);
                final CollectionItem updateItem = updateItem(states, position, collectionItem);

                if (updateItem != null) {
                    adapter.setItem(position, updateItem);
                }
            }
            // TODO move getRecentlyPlayedBucketRenderer().update(states) in the same loop
            adapter.getRecentlyPlayedBucketRenderer().update(states);
        }

        @Nullable
        private CollectionItem updateItem(OfflineProperties states, int position, CollectionItem collectionItem) {
            if (collectionItem.getType() == CollectionItem.TYPE_PREVIEW && adapter.getItems().size() > position) {
                return updateTrackLikedPreviewItem(states, (PreviewCollectionItem) collectionItem);
            } else if (collectionItem instanceof OfflineItem) {
                return updateOfflineItem(states, collectionItem);
            } else {
                return null;
            }
        }

        private CollectionItem updateOfflineItem(OfflineProperties states, CollectionItem collectionItem) {
            CollectionItem updateItem;
            final OfflineItem offlineItem = (OfflineItem) collectionItem;
            final OfflineState offlineState = states.state(collectionItem.getUrn());
            updateItem = (CollectionItem) offlineItem.updatedWithOfflineState(offlineState);
            return updateItem;
        }

        private CollectionItem updateTrackLikedPreviewItem(OfflineProperties states, PreviewCollectionItem collectionItem) {
            CollectionItem updateItem;
            final PreviewCollectionItem previewCollectionItem = collectionItem;
            updateItem = previewCollectionItem.updatedWithOfflineState(states.likedTracksState());
            return updateItem;
        }

    }
}
