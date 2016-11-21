package com.soundcloud.android.collection;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
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
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
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
        implements TrackItemRenderer.Listener, OnboardingItemCellRenderer.Listener, UpsellItemCellRenderer.Listener {

    private static final int FIXED_ITEMS = 5;

    private final Func1<Object, Boolean> isNotRefreshing = new Func1<Object, Boolean>() {
        @Override
        public Boolean call(Object event) {
            return !swipeRefreshAttacher.isRefreshing();
        }
    };

    private final Action1<MyCollection> clearOnNext = new Action1<MyCollection>() {
        @Override
        public void call(MyCollection myCollection) {
            adapter.clear();
        }
    };

    @VisibleForTesting
    final Func1<MyCollection, Iterable<CollectionItem>> toCollectionItems =
            new Func1<MyCollection, Iterable<CollectionItem>>() {
                @Override
                public List<CollectionItem> call(MyCollection myCollection) {
                    List<CollectionItem> collectionItems = buildCollectionItems(myCollection);
                    if (collectionOptionsStorage.isOnboardingEnabled()) {
                        return collectionWithOnboarding(collectionItems);
                    } else if (featureOperations.upsellOfflineContent() && collectionOptionsStorage.isUpsellEnabled()) {
                        return collectionWithUpsell(collectionItems);
                    } else {
                        return collectionItems;
                    }
                }
            };

    private final SwipeRefreshAttacher swipeRefreshAttacher;
    private final EventBus eventBus;
    private final CollectionAdapter adapter;
    private final Resources resources;
    private final CollectionOptionsStorage collectionOptionsStorage;
    private final FeatureOperations featureOperations;
    private final Navigator navigator;
    private final CollectionOperations collectionOperations;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final PlayHistoryOperations playHistoryOperations;

    private CompositeSubscription eventSubscriptions = new CompositeSubscription();


    @Inject
    CollectionPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                        CollectionOperations collectionOperations,
                        CollectionOptionsStorage collectionOptionsStorage,
                        CollectionAdapter adapter,
                        Resources resources,
                        EventBus eventBus,
                        Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                        PlayHistoryOperations playHistoryOperations,
                        FeatureOperations featureOperations,
                        Navigator navigator) {
        super(swipeRefreshAttacher);
        this.collectionOperations = collectionOperations;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.playHistoryOperations = playHistoryOperations;
        this.swipeRefreshAttacher = swipeRefreshAttacher;
        this.eventBus = eventBus;
        this.adapter = adapter;
        this.resources = resources;
        this.collectionOptionsStorage = collectionOptionsStorage;
        this.featureOperations = featureOperations;
        this.navigator = navigator;

        adapter.setTrackClickListener(this);
        adapter.setOnboardingListener(this);
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
    public void onDestroy(Fragment fragment) {
        eventSubscriptions.unsubscribe();
        super.onDestroy(fragment);
    }

    @Override
    protected CollectionBinding<MyCollection, CollectionItem> onBuildBinding(Bundle bundle) {
        final Observable<MyCollection> collections = collectionOperations.collections()
                                                                         .observeOn(AndroidSchedulers.mainThread());
        return CollectionBinding.from(collections.doOnNext(new OnCollectionLoadedAction()), toCollectionItems)
                                .withAdapter(adapter)
                                .build();
    }

    @Override
    protected CollectionBinding<MyCollection, CollectionItem> onRefreshBinding() {
        final Observable<MyCollection> collections =
                collectionOperations.updatedCollections().observeOn(AndroidSchedulers.mainThread());
        return CollectionBinding.from(collections.doOnError(new OnErrorAction()).doOnNext(clearOnNext),
                                      toCollectionItems)
                                .withAdapter(adapter)
                                .build();
    }

    @Override
    public void onCollectionsOnboardingItemClosed(int position) {
        collectionOptionsStorage.disableOnboarding();
        removeItem(position);
    }

    @Override
    public void onUpsellClose(int position) {
        collectionOptionsStorage.disableUpsell();
        removeItem(position);
    }

    @Override
    public void onUpsell(Context context) {
        navigator.openUpgrade(context);
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forCollectionClick());
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    private void refreshCollections() {
        final Observable<MyCollection> source = collectionOperations.collections()
                                                                    .observeOn(AndroidSchedulers.mainThread())
                                                                    .doOnNext(clearOnNext);
        retryWith(CollectionBinding
                          .from(source, toCollectionItems)
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
        adapter.notifyItemRemoved(position);
    }

    private void showError() {
        Toast.makeText(getRecyclerView().getContext(),
                       R.string.collections_loading_error,
                       Toast.LENGTH_LONG).show();
    }

    private void subscribeForUpdates() {
        eventSubscriptions.unsubscribe();
        eventSubscriptions = new CompositeSubscription(
                eventBus.subscribe(EventQueue.OFFLINE_CONTENT_CHANGED, new UpdateCollectionDownloadSubscriber(adapter)),
                collectionOperations.onCollectionChanged()
                                    .filter(isNotRefreshing)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new RefreshCollectionsSubscriber())
        );
    }

    private class RefreshCollectionsSubscriber extends DefaultSubscriber<Object> {
        @Override
        public void onNext(Object ignored) {
            refreshCollections();
        }
    }

    private class OnCollectionLoadedAction implements Action1<MyCollection> {
        @Override
        public void call(MyCollection myCollection) {
            adapter.clear();
            subscribeForUpdates();
            if (myCollection.hasError()) {
                showError();
            }
        }
    }

    private class OnErrorAction implements Action1<Throwable> {
        @Override
        public void call(Throwable ignored) {
            showError();
        }
    }

    private List<CollectionItem> collectionWithOnboarding(List<CollectionItem> collectionItems) {
        return prependItemToCollection(OnboardingCollectionItem.create(), collectionItems);
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
                                                                               myCollection.getPlaylistItems(),
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
        playHistoryOperations
                .startPlaybackFrom(urn, Screen.COLLECTIONS)
                .subscribe(expandPlayerSubscriberProvider.get());
    }


}
