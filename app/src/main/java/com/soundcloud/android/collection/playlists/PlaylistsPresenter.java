package com.soundcloud.android.collection.playlists;

import static com.soundcloud.android.events.EventQueue.LIKE_CHANGED;
import static com.soundcloud.android.events.EventQueue.PLAYLIST_CHANGED;
import static com.soundcloud.android.events.EventQueue.REPOST_CHANGED;
import static com.soundcloud.android.events.EventQueue.URN_STATE_CHANGED;

import com.soundcloud.android.R;
import com.soundcloud.android.collection.CollectionItemDecoration;
import com.soundcloud.android.collection.CollectionOptionsStorage;
import com.soundcloud.android.events.CollectionEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.PlaylistChangedEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.LikeEntityListSubscriber;
import com.soundcloud.android.view.adapters.RepostEntityListSubscriber;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class PlaylistsPresenter extends RecyclerViewPresenter<List<PlaylistCollectionItem>, PlaylistCollectionItem>
        implements PlaylistsAdapter.Listener, PlaylistOptionsPresenter.Listener, FilterHeaderPresenter.Listener {

    private final SwipeRefreshAttacher swipeRefreshAttacher;
    private final MyPlaylistsOperations myPlaylistsOperations;
    private final PlaylistsAdapter adapter;
    private final PlaylistOptionsPresenter optionsPresenter;
    private final Resources resources;
    private final EventBus eventBus;
    private final CollectionOptionsStorage collectionOptionsStorage;

    private PlaylistsOptions currentOptions;
    private CompositeSubscription eventSubscriptions = new CompositeSubscription();

    @Inject
    public PlaylistsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                              MyPlaylistsOperations myPlaylistsOperations,
                              CollectionOptionsStorage collectionOptionsStorage,
                              PlaylistsAdapter adapter,
                              PlaylistOptionsPresenter optionsPresenter,
                              Resources resources,
                              EventBus eventBus) {
        super(swipeRefreshAttacher);
        this.swipeRefreshAttacher = swipeRefreshAttacher;
        this.myPlaylistsOperations = myPlaylistsOperations;
        this.collectionOptionsStorage = collectionOptionsStorage;
        this.adapter = adapter;
        this.optionsPresenter = optionsPresenter;
        this.resources = resources;
        this.eventBus = eventBus;

        adapter.setHasStableIds(true);
        adapter.setListener(this);
        currentOptions = collectionOptionsStorage.getLastOrDefault();
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
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

    }

    @Override
    public void onFilterQuery(String query) {
        refreshWithNewOptions(PlaylistsOptions.builder(currentOptions).textFilter(query).build());
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        eventSubscriptions.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    protected CollectionBinding<List<PlaylistCollectionItem>, PlaylistCollectionItem> onBuildBinding(Bundle bundle) {
        return buildBinding(playlists());
    }

    @Override
    protected CollectionBinding<List<PlaylistCollectionItem>, PlaylistCollectionItem> onRefreshBinding() {
        return buildBinding(updatedPlaylists());
    }

    private CollectionBinding<List<PlaylistCollectionItem>, PlaylistCollectionItem> buildBinding(Observable<List<PlaylistItem>> source) {
        return CollectionBinding.from(source.map(this::playlistCollectionItems)
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .doOnNext(new OnCollectionLoadedAction()))
                                .withAdapter(adapter)
                                .build();
    }

    private Observable<List<PlaylistItem>> playlists() {
        return myPlaylistsOperations.myPlaylists(currentOptions);
    }

    private Observable<List<PlaylistItem>> updatedPlaylists() {
        return myPlaylistsOperations.refreshAndLoadPlaylists(currentOptions);
    }

    @Override
    public void onFilterOptionsClicked(Context context) {
        optionsPresenter.showOptions(context, this, currentOptions);
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
        refreshWithNewOptions(options);
        eventBus.publish(EventQueue.TRACKING, CollectionEvent.forFilter(currentOptions));
    }

    public void refreshWithNewOptions(PlaylistsOptions options) {
        currentOptions = options;
        refreshCollections();
    }

    private boolean isCurrentlyFiltered() {
        return Strings.isNotBlank(currentOptions.textFilter())
                || currentOptions.showOfflineOnly()
                || (currentOptions.showLikes() && !currentOptions.showPosts())
                || (!currentOptions.showLikes() && currentOptions.showPosts());
    }

    @VisibleForTesting
    List<PlaylistCollectionItem> playlistCollectionItems(List<PlaylistItem> playlistItems) {

        List<PlaylistCollectionItem> items = new ArrayList<>(playlistItems.size() + 2);

        items.add(PlaylistCollectionHeaderItem.create(playlistItems.size()));

        for (PlaylistItem playlistItem : playlistItems) {
            items.add(PlaylistCollectionPlaylistItem.create(playlistItem));
        }

        if (isCurrentlyFiltered()) {
            items.add(PlaylistCollectionRemoveFilterItem.create());
        } else if (playlistItems.isEmpty()) {
            items.add(PlaylistCollectionEmptyPlaylistItem.create());
        }

        return items;
    }

    private void refreshCollections() {
        retryWith(buildBinding(playlists()));
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

    private final Func1<Object, Boolean> isNotRefreshing = new Func1<Object, Boolean>() {
        @Override
        public Boolean call(Object event) {
            return !swipeRefreshAttacher.isRefreshing();
        }
    };

    private void subscribeForUpdates() {
        eventSubscriptions.unsubscribe();
        eventSubscriptions = new CompositeSubscription(
                eventBus.subscribe(EventQueue.OFFLINE_CONTENT_CHANGED, new UpdatePlaylistsDownloadSubscriber()),
                eventBus.queue(PLAYLIST_CHANGED)
                        .filter(isNotRefreshing)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new DefaultSubscriber<PlaylistChangedEvent>() {
                            @Override
                            public void onNext(PlaylistChangedEvent args) {
                                updateFromPlaylistChange(args);
                            }
                        }),
                eventBus.queue(URN_STATE_CHANGED)
                        .filter(UrnStateChangedEvent::containsPlaylist)
                        .filter(isNotRefreshing)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new DefaultSubscriber<UrnStateChangedEvent>() {
                            @Override
                            public void onNext(UrnStateChangedEvent args) {
                                refreshCollections();
                            }
                        }),
                eventBus.queue(LIKE_CHANGED)
                        .filter(LikesStatusEvent::containsPlaylistChange)
                        .filter(isNotRefreshing)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new LikeEntityListSubscriber(adapter)),
                eventBus.queue(REPOST_CHANGED)
                        .filter(RepostsStatusEvent::containsPlaylistChange)
                        .filter(isNotRefreshing)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new RepostEntityListSubscriber(adapter))
        );
    }

    private void updateFromPlaylistChange(PlaylistChangedEvent event) {
        final Set<Urn> urns = PlaylistChangedEvent.TO_URNS.call(event);
        if (urns.size() == 1) {
            boolean updated = false;
            final Urn urn = urns.iterator().next();
            for (int position = 0; position < adapter.getItems().size(); position++) {
                PlaylistCollectionItem item = adapter.getItem(position);

                if (item.getType() == PlaylistCollectionItem.TYPE_PLAYLIST && item.getUrn().equals(urn)) {
                    final PlaylistCollectionPlaylistItem playlistItem = (PlaylistCollectionPlaylistItem) item;
                    if (position < adapter.getItems().size()) {
                        adapter.setItem(position, (PlaylistCollectionPlaylistItem) event.apply(playlistItem));
                        updated = true;
                    }
                }
            }
            if (updated) {
                adapter.notifyDataSetChanged();
            }
        } else {
            refreshCollections();
        }
    }

    private class OnCollectionLoadedAction implements Action1<List<PlaylistCollectionItem>> {
        @Override
        public void call(List<PlaylistCollectionItem> ignored) {
            adapter.clear();
            subscribeForUpdates();
        }
    }

    private class UpdatePlaylistsDownloadSubscriber extends DefaultSubscriber<OfflineContentChangedEvent> {
        @Override
        public void onNext(final OfflineContentChangedEvent event) {
            boolean isUpdated = false;
            for (int position = 0; position < adapter.getItems().size(); position++) {
                PlaylistCollectionItem item = adapter.getItem(position);
                if (item.getType() == PlaylistCollectionItem.TYPE_PLAYLIST && event.entities.contains(item.getUrn())) {
                    final PlaylistCollectionPlaylistItem playlistItem = (PlaylistCollectionPlaylistItem) item;
                    if (position < adapter.getItems().size()) {
                        isUpdated = true;
                        adapter.setItem(position, playlistItem.updatedWithOfflineState(event.state));
                    }
                }
            }
            if (isUpdated) {
                adapter.notifyDataSetChanged();
            }
        }
    }

}
