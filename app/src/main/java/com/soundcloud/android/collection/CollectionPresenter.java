package com.soundcloud.android.collection;

import com.soundcloud.android.R;
import com.soundcloud.android.events.CollectionEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PullToRefreshEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.view.View;
import android.widget.Toast;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class CollectionPresenter extends RecyclerViewPresenter<CollectionItem> implements CollectionAdapter.Listener, CollectionPlaylistOptionsPresenter.Listener, OnboardingItemCellRenderer.Listener {

    private static final int NON_PLAYLIST_COLLECTION_ITEMS = 4;

    @VisibleForTesting
    final Func1<MyCollection, Iterable<CollectionItem>> toCollectionItems =
            new Func1<MyCollection, Iterable<CollectionItem>>() {
                @Override
                public List<CollectionItem> call(MyCollection myCollection) {
                    List<PlaylistItem> playlistItems = myCollection.getPlaylistItems();
                    List<CollectionItem> collectionItems = new ArrayList<>(playlistItems.size() + NON_PLAYLIST_COLLECTION_ITEMS);

                    if (collectionOptionsStorage.isOnboardingEnabled()) {
                        collectionItems.add(CollectionItem.fromOnboarding());
                    }

                    collectionItems.add(CollectionItem.fromCollectionsPreview(myCollection.getLikes(), myCollection.getRecentStations()));

                    collectionItems.add(CollectionItem.fromPlaylistHeader());

                    for (PlaylistItem playlistItem : playlistItems) {
                        collectionItems.add(CollectionItem.fromPlaylistItem(playlistItem));
                    }

                    // TODO. We should test this once we know the rules
                    if (isCurrentlyFiltered()) {
                        collectionItems.add(CollectionItem.fromKillFilter());
                    } else if (playlistItems.isEmpty()) {
                        collectionItems.add(CollectionItem.fromEmptyPlaylists());
                    }

                    return collectionItems;
                }
            };

    private final Func1<Object, Boolean> isNotRefreshing = new Func1<Object, Boolean>() {
        @Override
        public Boolean call(Object event) {
            return !swipeRefreshAttacher.isRefreshing();
        }
    };

    private final Action1<Object> logCollectionChanged = new Action1<Object>() {
        @Override
        public void call(Object o) {
            Log.d(this, "OnCollectionChanged [event=" + o + ", isNotRefreshing=" + isNotRefreshing + "]");
        }
    };

    private final SwipeRefreshAttacher swipeRefreshAttacher;
    private final CollectionOperations collectionOperations;
    private final CollectionOptionsStorage collectionOptionsStorage;
    private final CollectionAdapter adapter;
    private final CollectionPlaylistOptionsPresenter optionsPresenter;
    private final Resources resources;
    private final EventBus eventBus;

    private CompositeSubscription eventSubscriptions = new CompositeSubscription();
    private PlaylistsOptions currentOptions;

    private final Action1<MyCollection> clearOnNext = new Action1<MyCollection>() {
        @Override
        public void call(MyCollection myCollection) {
            adapter.clear();
        }
    };

    @Inject
    CollectionPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                        CollectionOperations collectionOperations,
                        CollectionOptionsStorage collectionOptionsStorage,
                        CollectionAdapter adapter,
                        CollectionPlaylistOptionsPresenter optionsPresenter,
                        Resources resources,
                        EventBus eventBus) {
        super(swipeRefreshAttacher, Options.defaults());
        this.swipeRefreshAttacher = swipeRefreshAttacher;
        this.collectionOperations = collectionOperations;
        this.collectionOptionsStorage = collectionOptionsStorage;
        this.adapter = adapter;
        this.optionsPresenter = optionsPresenter;
        this.resources = resources;
        this.eventBus = eventBus;
        adapter.setListener(this);
        adapter.setOnboardingListener(this);
        currentOptions = collectionOptionsStorage.getLastOrDefault();
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        final int spanCount = resources.getInteger(R.integer.collection_grid_span_count);
        final GridLayoutManager layoutManager = new GridLayoutManager(view.getContext(), spanCount);
        layoutManager.setSpanSizeLookup(createSpanSizeLookup(spanCount));
        getRecyclerView().setLayoutManager(layoutManager);
    }

    @Override
    public void onDestroy(Fragment fragment) {
        eventSubscriptions.unsubscribe();
        super.onDestroy(fragment);
    }

    @Override
    public void onPlaylistSettingsClicked(View view) {
        optionsPresenter.showOptions(view.getContext(), this, currentOptions);
    }

    @Override
    public void onRemoveFilterClicked() {
        onOptionsUpdated(PlaylistsOptions.builder().build());
        eventBus.publish(EventQueue.TRACKING, CollectionEvent.forClearFilter());
    }

    @Override
    public void onOptionsUpdated(PlaylistsOptions options) {
        collectionOptionsStorage.store(options);
        currentOptions = options;
        refreshCollections();
        eventBus.publish(EventQueue.TRACKING, CollectionEvent.forFilter(currentOptions));
    }

    private void refreshCollections() {
        final Observable<MyCollection> source = collectionOperations
                .collections(currentOptions)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(clearOnNext);
        retryWith(CollectionBinding
                .from(source, toCollectionItems)
                .withAdapter(adapter).build());
    }

    @Override
    public void onCollectionsOnboardingItemClosed(int position) {
        collectionOptionsStorage.disableOnboarding();
        removeItem(position);
    }

    @NonNull
    private GridLayoutManager.SpanSizeLookup createSpanSizeLookup(final int spanCount) {
        return new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position < adapter.getItemCount() && adapter.getItem(position).isPlaylistItem()) {
                    return 1;
                } else {
                    return spanCount;
                }
            }
        };
    }

    @Override
    protected CollectionBinding<CollectionItem> onBuildBinding(Bundle bundle) {
        final Observable<MyCollection> collections = collectionOperations.collections(currentOptions).observeOn(AndroidSchedulers.mainThread());
        return CollectionBinding.from(collections.doOnNext(new OnCollectionLoadedAction()), toCollectionItems)
                .withAdapter(adapter)
                .build();
    }

    @Override
    protected CollectionBinding<CollectionItem> onRefreshBinding() {
        final Observable<MyCollection> collections =
                collectionOperations.updatedCollections(currentOptions)
                        .doOnSubscribe(eventBus.publishAction0(EventQueue.TRACKING, new PullToRefreshEvent(Screen.COLLECTIONS)))
                        .observeOn(AndroidSchedulers.mainThread());
        return CollectionBinding.from(collections.doOnError(new OnErrorAction()).doOnNext(clearOnNext), toCollectionItems)
                .withAdapter(adapter)
                .build();
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    private boolean isCurrentlyFiltered() {
        return (currentOptions.showLikes() && !currentOptions.showPosts())
                || !currentOptions.showLikes() && currentOptions.showPosts();
    }

    private void removeItem(int position) {
        adapter.removeItem(position);
        adapter.notifyItemRemoved(position);
    }

    private class RefreshCollectionsSubscriber extends DefaultSubscriber<Object> {
        @Override
        public void onNext(Object ignored) {
            refreshCollections();
        }
    }

    private class OnErrorAction implements Action1<Throwable> {
        @Override
        public void call(Throwable ignored) {
            showError();
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

    private void subscribeForUpdates() {
        eventSubscriptions.unsubscribe();
        eventSubscriptions = new CompositeSubscription(
                eventBus.subscribe(EventQueue.OFFLINE_CONTENT_CHANGED, new UpdateCollectionDownloadSubscriber(adapter)),
                collectionOperations.onCollectionChanged()
                        .doOnNext(logCollectionChanged)
                        .filter(isNotRefreshing)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new RefreshCollectionsSubscriber())
        );
    }

    private void showError() {
        Toast.makeText(getRecyclerView().getContext(),
                R.string.collections_loading_error,
                Toast.LENGTH_LONG).show();
    }

}
