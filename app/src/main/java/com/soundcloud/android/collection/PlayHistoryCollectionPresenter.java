package com.soundcloud.android.collection;

import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PlayHistoryCollectionPresenter extends BaseCollectionPresenter {

    private final CollectionOperations collectionOperations;

    public PlayHistoryCollectionPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                                          CollectionOperations collectionOperations,
                                          CollectionOptionsStorage collectionOptionsStorage,
                                          CollectionAdapter adapter,
                                          Resources resources,
                                          EventBus eventBus) {
        super(swipeRefreshAttacher, eventBus, adapter, resources, collectionOptionsStorage);
        this.collectionOperations = collectionOperations;
    }

    @Override
    protected Observable<MyCollection> myCollection() {
        return collectionOperations.collectionsForPlayHistory();
    }

    @Override
    protected Observable<MyCollection> updatedMyCollection() {
        return collectionOperations.updatedCollectionsForPlayHistory();
    }

    @Override
    protected Observable<Object> onCollectionChanged() {
        return collectionOperations.onCollectionChangedWithPlayHistory();
    }

    @Override
    protected List<CollectionItem> buildCollectionItems(MyCollection myCollection) {
        List<TrackItem> playHistoryTrackItems = myCollection.getPlayHistoryTrackItems();
        List<RecentlyPlayedItem> recentlyPlayedItems = myCollection.getRecentlyPlayedItems();
        List<CollectionItem> collectionItems = new ArrayList<>(playHistoryTrackItems.size() + recentlyPlayedItems.size() + 4);

        collectionItems.add(PreviewCollectionItem.forLikesAndPlaylists(myCollection.getLikes(),
                                                                       myCollection.getPlaylistItems()));

        if (recentlyPlayedItems.size() > 0) {
            collectionItems.addAll(recentlyPlayedItems(recentlyPlayedItems));
        }

        if (playHistoryTrackItems.size() > 0) {
            collectionItems.addAll(playHistoryCollectionItems(playHistoryTrackItems));
        }

        return collectionItems;
    }

    private Collection<CollectionItem> recentlyPlayedItems(List<RecentlyPlayedItem> recentlyPlayedItems) {
        List<CollectionItem> items = new ArrayList<>(recentlyPlayedItems.size() + 2);

        items.add(HeaderCollectionItem.forRecentlyPlayed());

        for (RecentlyPlayedItem item : recentlyPlayedItems) {
            items.add(RecentlyPlayedCollectionItem.create(item));
        }

        items.add(ViewAllCollectionItem.forRecentlyPlayed());

        return items;
    }

    private List<CollectionItem> playHistoryCollectionItems(List<TrackItem> playHistoryTrackItems) {
        List<CollectionItem> items = new ArrayList<>(playHistoryTrackItems.size() + 2);

        items.add(HeaderCollectionItem.forPlayHistory());

        for (TrackItem trackItem : playHistoryTrackItems) {
            items.add(TrackCollectionItem.create(trackItem));
        }

        items.add(ViewAllCollectionItem.forPlayHistory());

        return items;
    }

}
