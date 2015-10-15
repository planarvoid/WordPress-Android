package com.soundcloud.android.collections;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.UpdateCurrentDownloadSubscriber;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.rx.eventbus.EventBus;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

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

public class CollectionsPresenter extends RecyclerViewPresenter<CollectionsItem> implements CollectionsAdapter.Listener, CollectionsPlaylistOptionsPresenter.Listener, OnboardingItemCellRenderer.Listener {

    @VisibleForTesting
    final Func1<MyCollections, Iterable<CollectionsItem>> toCollectionsItems =
            new Func1<MyCollections, Iterable<CollectionsItem>>() {
                @Override
                public List<CollectionsItem> call(MyCollections myCollections) {
                    List<PlaylistItem> playlistItems = myCollections.getPlaylistItems();
                    List<CollectionsItem> collectionsItems = new ArrayList<>(playlistItems.size() + 4);

                    if (featureFlags.isEnabled(Flag.STATIONS_SOFT_LAUNCH)) {
                        if (collectionsOptionsStorage.isOnboardingEnabled()) {
                            collectionsItems.add(CollectionsItem.fromOnboarding());
                        }
                    }

                    if (!myCollections.getLikes().isEmpty() || !myCollections.getRecentStations().isEmpty()) {
                        collectionsItems.add(CollectionsItem.fromCollectionsPreview(myCollections.getLikes(), myCollections.getRecentStations()));
                    }

                    collectionsItems.add(CollectionsItem.fromPlaylistHeader());

                    for (PlaylistItem playlistItem : playlistItems) {
                        collectionsItems.add(CollectionsItem.fromPlaylistItem(playlistItem));
                    }

                    // TODO. We should test this once we know the rules
                    if (isCurrentlyFiltered()) {
                        collectionsItems.add(CollectionsItem.fromKillFilter());
                    } else if (playlistItems.isEmpty()) {
                        collectionsItems.add(CollectionsItem.fromEmptyPlaylists());
                    }

                    return collectionsItems;
                }
            };

    private final CollectionsOperations collectionsOperations;
    private final CollectionsOptionsStorage collectionsOptionsStorage;
    private final CollectionsAdapter adapter;
    private final CollectionsPlaylistOptionsPresenter optionsPresenter;
    private final Resources resources;
    private final EventBus eventBus;
    private final FeatureFlags featureFlags;

    private CompositeSubscription eventSubscriptions;
    private PlaylistsOptions currentOptions;

    @Inject
    CollectionsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                         CollectionsOperations collectionsOperations,
                         CollectionsOptionsStorage collectionsOptionsStorage,
                         CollectionsAdapter adapter,
                         CollectionsPlaylistOptionsPresenter optionsPresenter,
                         Resources resources,
                         EventBus eventBus,
                         FeatureFlags featureFlags) {
        super(swipeRefreshAttacher, Options.defaults());
        this.collectionsOperations = collectionsOperations;
        this.collectionsOptionsStorage = collectionsOptionsStorage;
        this.adapter = adapter;
        this.optionsPresenter = optionsPresenter;
        this.resources = resources;
        this.eventBus = eventBus;
        this.featureFlags = featureFlags;
        adapter.setListener(this);
        adapter.setOnboardingListener(this);
        currentOptions = collectionsOptionsStorage.getLastOrDefault();
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();

        eventSubscriptions = new CompositeSubscription(
                eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter)),
                eventBus.subscribe(EventQueue.CURRENT_DOWNLOAD, new UpdateCurrentDownloadSubscriber(adapter))
        );
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        RecyclerView recyclerView = getRecyclerView();
        final int spanCount = resources.getInteger(R.integer.stations_grid_span_count);
        final GridLayoutManager layoutManager = new GridLayoutManager(view.getContext(), spanCount);
        layoutManager.setSpanSizeLookup(createSpanSizeLookup(spanCount));
        recyclerView.setLayoutManager(layoutManager);
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
    }

    @Override
    public void onOptionsUpdated(PlaylistsOptions options) {
        collectionsOptionsStorage.store(options);
        currentOptions = options;
        adapter.clear();
        retryWith(onBuildBinding(null));
    }

    @Override
    public void onCollectionsOnboardingItemClosed(int position) {
        collectionsOptionsStorage.disableOnboarding();
        removeItem(position);
    }

    @NonNull
    private GridLayoutManager.SpanSizeLookup createSpanSizeLookup(final int spanCount) {
        return new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.getItem(position).isPlaylistItem() ? 1 : spanCount;
            }
        };
    }

    @Override
    protected CollectionBinding<CollectionsItem> onBuildBinding(Bundle bundle) {
        return CollectionBinding.from(collectionsOperations.collections(currentOptions), toCollectionsItems)
                .withAdapter(adapter)
                .build();
    }

    @Override
    protected CollectionBinding<CollectionsItem> onRefreshBinding() {
        return CollectionBinding.from(collectionsOperations.updatedCollections(currentOptions), toCollectionsItems)
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
}
