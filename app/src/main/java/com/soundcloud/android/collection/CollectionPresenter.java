package com.soundcloud.android.collection;

import com.soundcloud.android.events.CollectionEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;

import android.content.res.Resources;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class CollectionPresenter extends BaseCollectionPresenter
        implements CollectionAdapter.Listener, CollectionPlaylistOptionsPresenter.Listener {

    private static final int NON_PLAYLIST_OR_TRACK_COLLECTION_ITEMS = 6;

    private final CollectionOperations collectionOperations;
    private final CollectionPlaylistOptionsPresenter optionsPresenter;
    private final EventBus eventBus;
    private final CollectionOptionsStorage collectionOptionsStorage;

    private PlaylistsOptions currentOptions;

    public CollectionPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                               CollectionOperations collectionOperations,
                               CollectionOptionsStorage collectionOptionsStorage,
                               CollectionAdapter adapter,
                               CollectionPlaylistOptionsPresenter optionsPresenter,
                               Resources resources,
                               EventBus eventBus) {
        super(swipeRefreshAttacher, eventBus, adapter, resources, collectionOptionsStorage);
        this.collectionOperations = collectionOperations;
        this.collectionOptionsStorage = collectionOptionsStorage;
        this.optionsPresenter = optionsPresenter;
        this.eventBus = eventBus;
        adapter.setListener(this);
        currentOptions = collectionOptionsStorage.getLastOrDefault();
    }

    @Override
    public Observable<MyCollection> myCollection() {
        return collectionOperations.collections(currentOptions);
    }

    @Override
    public Observable<MyCollection> updatedMyCollection() {
        return collectionOperations.updatedCollections(currentOptions);
    }

    @Override
    public Observable<Object> onCollectionChanged() {
        return collectionOperations.onCollectionChanged();
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

    private boolean isCurrentlyFiltered() {
        return currentOptions.showOfflineOnly()
                || (currentOptions.showLikes() && !currentOptions.showPosts())
                || (!currentOptions.showLikes() && currentOptions.showPosts());
    }

    protected List<CollectionItem> buildCollectionItems(MyCollection myCollection) {
        List<TrackItem> playHistoryTrackItems = myCollection.getPlayHistoryTrackItems();
        List<PlaylistItem> playlistItems = myCollection.getPlaylistItems();
        List<CollectionItem> collectionItems = new ArrayList<>(playlistItems.size() +
                                                                       playHistoryTrackItems.size() + NON_PLAYLIST_OR_TRACK_COLLECTION_ITEMS);

        collectionItems.add(PreviewCollectionItem.forLikesAndStations(myCollection.getLikes(),
                                                                      myCollection.getRecentStations()));
        collectionItems.addAll(playlistCollectionItems(playlistItems, false));

        return collectionItems;
    }

    protected List<CollectionItem> playlistCollectionItems(List<PlaylistItem> playlistItems, boolean withTopSpacing) {

        List<CollectionItem> items = new ArrayList<>(playlistItems.size() + 2);

        items.add(PlaylistHeaderCollectionItem.create(playlistItems.size(), withTopSpacing));

        for (PlaylistItem playlistItem : playlistItems) {
            items.add(PlaylistCollectionItem.create(playlistItem));
        }

        if (isCurrentlyFiltered()) {
            items.add(PlaylistRemoveFilterCollectionItem.create());
        } else if (playlistItems.isEmpty()) {
            items.add(EmptyPlaylistCollectionItem.create());
        }

        return items;
    }

}
