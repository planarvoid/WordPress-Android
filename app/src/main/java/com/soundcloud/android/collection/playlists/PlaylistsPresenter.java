package com.soundcloud.android.collection.playlists;

import static com.soundcloud.android.events.EventQueue.LIKE_CHANGED;
import static com.soundcloud.android.events.EventQueue.PLAYLIST_CHANGED;
import static com.soundcloud.android.events.EventQueue.REPOST_CHANGED;
import static com.soundcloud.android.events.EventQueue.URN_STATE_CHANGED;
import static com.soundcloud.android.rx.observers.LambdaSubscriber.onNext;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricParams;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
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
import com.soundcloud.android.offline.OfflineProperties;
import com.soundcloud.android.offline.OfflinePropertiesProvider;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.OfflineItem;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.rx.observers.LambdaObserver;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.LikeEntityListObserver;
import com.soundcloud.android.view.adapters.RepostEntityListObserver;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import org.jetbrains.annotations.Nullable;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
        implements PlaylistsAdapter.Listener, PlaylistOptionsPresenter.Listener {

    private final SwipeRefreshAttacher swipeRefreshAttacher;
    private final MyPlaylistsOperations myPlaylistsOperations;
    private final PlaylistsAdapter adapter;
    private final PlaylistOptionsPresenter optionsPresenter;
    private final Resources resources;
    private final EventBusV2 eventBus;
    private final OfflinePropertiesProvider offlinePropertiesProvider;
    private final FeatureFlags featureFlags;
    private final CollectionOptionsStorage collectionOptionsStorage;
    private final EntityItemCreator entityItemCreator;
    private final PerformanceMetricsEngine performanceMetricsEngine;
    private final CompositeDisposable eventDisposables = new CompositeDisposable();

    private PlaylistsOptions.Entities entities;
    private PlaylistsOptions currentOptions;

    @Inject
    public PlaylistsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                              MyPlaylistsOperations myPlaylistsOperations,
                              CollectionOptionsStorage collectionOptionsStorage,
                              PlaylistsAdapter adapter,
                              PlaylistOptionsPresenter optionsPresenter,
                              Resources resources,
                              EventBusV2 eventBus,
                              OfflinePropertiesProvider offlinePropertiesProvider,
                              FeatureFlags featureFlags,
                              EntityItemCreator entityItemCreator,
                              PerformanceMetricsEngine performanceMetricsEngine) {
        super(swipeRefreshAttacher, new Options.Builder()
                .useDividers(Options.DividerMode.NONE)
                .build());
        this.swipeRefreshAttacher = swipeRefreshAttacher;
        this.myPlaylistsOperations = myPlaylistsOperations;
        this.collectionOptionsStorage = collectionOptionsStorage;
        this.adapter = adapter;
        this.optionsPresenter = optionsPresenter;
        this.resources = resources;
        this.eventBus = eventBus;
        this.offlinePropertiesProvider = offlinePropertiesProvider;
        this.featureFlags = featureFlags;
        this.entityItemCreator = entityItemCreator;
        this.performanceMetricsEngine = performanceMetricsEngine;

        adapter.setHasStableIds(true);
        adapter.setListener(this);
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        this.entities = PlaylistsArguments.entities(fragment.getArguments());
        this.currentOptions = buildPlaylistOptionsWithFilters(collectionOptionsStorage.getLastOrDefault());
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
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        eventDisposables.clear();
        super.onDestroyView(fragment);
    }

    @Override
    protected CollectionBinding<List<PlaylistCollectionItem>, PlaylistCollectionItem> onBuildBinding(Bundle bundle) {
        return CollectionBinding.fromV2(setupSourceObservable(playlists()))
                                .withAdapter(adapter)
                                .addObserver(onNext(this::endMeasuringPlaylistsLoading))
                                .build();
    }

    @Override
    protected CollectionBinding<List<PlaylistCollectionItem>, PlaylistCollectionItem> onRefreshBinding() {
        return CollectionBinding.fromV2(setupSourceObservable(updatedPlaylists()))
                                .withAdapter(adapter)
                                .build();
    }

    @NonNull
    private Maybe<List<PlaylistCollectionItem>> setupSourceObservable(Maybe<List<PlaylistItem>> source) {
        return source.map(this::playlistCollectionItems)
                     .observeOn(AndroidSchedulers.mainThread())
                     .doOnSuccess(new OnCollectionLoadedAction());
    }

    private void endMeasuringPlaylistsLoading(Iterable<PlaylistCollectionItem> items) {
        int itemsCount = Iterables.size(items);
        PerformanceMetric performanceMetric = PerformanceMetric.builder().metricType(MetricType.PLAYLISTS_LOAD)
                                                               .metricParams(MetricParams.of(MetricKey.PLAYLISTS_COUNT, itemsCount))
                                                               .build();
        performanceMetricsEngine.endMeasuring(performanceMetric);
    }

    private Maybe<List<PlaylistItem>> playlists() {
        return myPlaylistsOperations.myPlaylists(currentOptions).map(toPlaylistsItems());
    }

    private Maybe<List<PlaylistItem>> updatedPlaylists() {
        return myPlaylistsOperations.refreshAndLoadPlaylists(currentOptions).map(toPlaylistsItems());
    }

    private Function<List<Playlist>, List<PlaylistItem>> toPlaylistsItems() {
        return playlists -> Lists.transform(playlists, entityItemCreator::playlistItem);
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
        currentOptions = buildPlaylistOptionsWithFilters(options);
        refreshCollections();
        eventBus.publish(EventQueue.TRACKING, CollectionEvent.forFilter(currentOptions));
    }

    private PlaylistsOptions buildPlaylistOptionsWithFilters(PlaylistsOptions options) {
        return PlaylistsOptions.builder(options).entities(entities).build();
    }

    private boolean isCurrentlyFiltered() {
        return currentOptions.showOfflineOnly()
                || (currentOptions.showLikes() && !currentOptions.showPosts())
                || (!currentOptions.showLikes() && currentOptions.showPosts());
    }

    @VisibleForTesting
    List<PlaylistCollectionItem> playlistCollectionItems(List<PlaylistItem> playlistItems) {

        List<PlaylistCollectionItem> items = new ArrayList<>(playlistItems.size() + 2);

        items.add(createCollectionHeaderItem(playlistItems.size()));

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

    @NonNull
    private PlaylistCollectionHeaderItem createCollectionHeaderItem(int entityCount) {
        if (entities == PlaylistsOptions.Entities.PLAYLISTS) {
            return PlaylistCollectionHeaderItem.createForPlaylists(entityCount);
        } else if (entities == PlaylistsOptions.Entities.ALBUMS) {
            return PlaylistCollectionHeaderItem.createForAlbums(entityCount);
        } else {
            return PlaylistCollectionHeaderItem.create(entityCount);
        }
    }

    private void refreshCollections() {
        retryWith(onBuildBinding(null));
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

    private final Predicate<Object> isNotRefreshing = new Predicate<Object>() {
        @Override
        public boolean test(Object event) {
            return !swipeRefreshAttacher.isRefreshing();
        }
    };

    private void subscribeForUpdates() {
        eventDisposables.clear();
        eventDisposables.addAll(
                subscribeToOfflineContent(),
                eventBus.queue(PLAYLIST_CHANGED)
                        .filter(isNotRefreshing)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(LambdaObserver.onNext(this::updateFromPlaylistChange)),
                eventBus.queue(URN_STATE_CHANGED)
                        .filter(isNotRefreshing)
                        .filter(UrnStateChangedEvent::containsPlaylist)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(LambdaObserver.onNext(args -> refreshCollections())),
                eventBus.queue(LIKE_CHANGED)
                        .filter(LikesStatusEvent::containsPlaylistChange)
                        .filter(isNotRefreshing)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new LikeEntityListObserver(adapter)),
                eventBus.queue(REPOST_CHANGED)
                        .filter(RepostsStatusEvent::containsPlaylistChange)
                        .filter(isNotRefreshing)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new RepostEntityListObserver(adapter))
        );
    }

    private Disposable subscribeToOfflineContent() {
        if (featureFlags.isEnabled(Flag.OFFLINE_PROPERTIES_PROVIDER)) {
            return offlinePropertiesProvider.states()
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribeWith(new OfflinePropertiesObserver());
        } else {
            return eventBus.subscribe(EventQueue.OFFLINE_CONTENT_CHANGED, new UpdatePlaylistsDownloadObserver());
        }
    }

    private void updateFromPlaylistChange(PlaylistChangedEvent event) {
        final Set<Urn> urns = PlaylistChangedEvent.TO_URNS.call(event);
        if (urns.size() == 1) {
            final Urn urn = urns.iterator().next();
            for (int position = 0; position < adapter.getItems().size(); position++) {
                PlaylistCollectionItem item = adapter.getItem(position);

                if (item.getType() == PlaylistCollectionItem.TYPE_PLAYLIST && item.getUrn().equals(urn)) {
                    final PlaylistCollectionPlaylistItem playlistItem = (PlaylistCollectionPlaylistItem) item;
                    if (position < adapter.getItems().size()) {
                        adapter.setItem(position, (PlaylistCollectionPlaylistItem) event.apply(playlistItem));
                    }
                }
            }
        } else {
            refreshCollections();
        }
    }

    private class OnCollectionLoadedAction implements Consumer<List<PlaylistCollectionItem>> {
        @Override
        public void accept(List<PlaylistCollectionItem> ignored) {
            adapter.clear();
            subscribeForUpdates();
        }
    }

    private class UpdatePlaylistsDownloadObserver extends DefaultObserver<OfflineContentChangedEvent> {
        @Override
        public void onNext(final OfflineContentChangedEvent event) {
            for (int position = 0; position < adapter.getItems().size(); position++) {
                PlaylistCollectionItem item = adapter.getItem(position);
                if (item.getType() == PlaylistCollectionItem.TYPE_PLAYLIST && event.entities.contains(item.getUrn())) {
                    final PlaylistCollectionPlaylistItem playlistItem = (PlaylistCollectionPlaylistItem) item;
                    if (position < adapter.getItems().size()) {
                        adapter.setItem(position, playlistItem.updatedWithOfflineState(event.state));
                    }
                }
            }
        }
    }

    private class OfflinePropertiesObserver extends DefaultObserver<OfflineProperties> {
        @Override
        public void onNext(OfflineProperties properties) {
            for (int position = 0; position < adapter.getItems().size(); position++) {
                PlaylistCollectionItem item = adapter.getItem(position);
                if (item instanceof OfflineItem) {
                    final OfflineItem offlineItem = (OfflineItem) item;
                    final OfflineState offlineState = properties.state(item.getUrn());
                    final ListItem updatedOfflineItem = offlineItem.updatedWithOfflineState(offlineState);
                    adapter.setItem(position, (PlaylistCollectionItem) updatedOfflineItem);
                }
            }
        }
    }
}
