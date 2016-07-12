package com.soundcloud.android.collection;

import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedBucketCollectionItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PlayHistoryCollectionPresenter extends BaseCollectionPresenter {

    private static final int FIXED_PLAY_HISTORY_ITEMS = 2;
    private static final int FIXED_RECENTLY_PLAYED_ITEMS = 2;
    private static final int FIXED_PREVIEW_ITEMS = 1;
    private static final int FIXED_ITEMS = FIXED_PREVIEW_ITEMS + FIXED_RECENTLY_PLAYED_ITEMS + FIXED_PLAY_HISTORY_ITEMS;

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
        List<CollectionItem> collectionItems = new ArrayList<>(playHistoryTrackItems.size() + FIXED_ITEMS);

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
        List<CollectionItem> items = new ArrayList<>(FIXED_RECENTLY_PLAYED_ITEMS);

        items.add(RecentlyPlayedBucketCollectionItem.create(recentlyPlayedItems));
        items.add(ViewAllCollectionItem.forRecentlyPlayed());

        return items;
    }

    private List<CollectionItem> playHistoryCollectionItems(List<TrackItem> playHistoryTrackItems) {
        List<CollectionItem> items = new ArrayList<>(playHistoryTrackItems.size() + FIXED_PLAY_HISTORY_ITEMS);

        items.add(HeaderCollectionItem.forPlayHistory());

        for (TrackItem trackItem : playHistoryTrackItems) {
            items.add(TrackCollectionItem.create(trackItem));
        }

        items.add(ViewAllCollectionItem.forPlayHistory());

        return items;
    }

}
