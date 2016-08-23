package com.soundcloud.android.collection.playhistory;

import com.soundcloud.android.collection.BaseCollectionPresenter;
import com.soundcloud.android.collection.CollectionAdapter;
import com.soundcloud.android.collection.CollectionItem;
import com.soundcloud.android.collection.CollectionOperations;
import com.soundcloud.android.collection.CollectionOptionsStorage;
import com.soundcloud.android.collection.MyCollection;
import com.soundcloud.android.collection.PreviewCollectionItem;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedBucketItem;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedPlayableItem;
import com.soundcloud.android.configuration.experiments.PlayHistoryExperiment;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.List;

public class PlayHistoryCollectionPresenter extends BaseCollectionPresenter {

    private static final int FIXED_PLAY_HISTORY_ITEMS = 2;
    private static final int FIXED_RECENTLY_PLAYED_ITEMS = 2;
    private static final int FIXED_PREVIEW_ITEMS = 1;
    private static final int FIXED_ITEMS = FIXED_PREVIEW_ITEMS + FIXED_RECENTLY_PLAYED_ITEMS + FIXED_PLAY_HISTORY_ITEMS;

    private final CollectionOperations collectionOperations;
    private final PlayHistoryExperiment experiment;

    public PlayHistoryCollectionPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                                          CollectionOperations collectionOperations,
                                          CollectionOptionsStorage collectionOptionsStorage,
                                          CollectionAdapter adapter,
                                          PlayHistoryExperiment experiment,
                                          Resources resources,
                                          EventBus eventBus) {
        super(swipeRefreshAttacher, eventBus, adapter, resources, collectionOptionsStorage);
        this.collectionOperations = collectionOperations;
        this.experiment = experiment;
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
        List<RecentlyPlayedPlayableItem> recentlyPlayedPlayableItems = myCollection.getRecentlyPlayedItems();
        List<CollectionItem> collectionItems = new ArrayList<>(playHistoryTrackItems.size() + FIXED_ITEMS);

        collectionItems.add(PreviewCollectionItem.forLikesAndPlaylists(myCollection.getLikes(),
                                                                       myCollection.getPlaylistItems()));

        if (experiment.showOnlyOnSearch()) {
            addPlayHistory(playHistoryTrackItems, collectionItems);
        } else if (experiment.showBelowListeningHistory()) {
            addPlayHistory(playHistoryTrackItems, collectionItems);
            addRecentlyPlayed(recentlyPlayedPlayableItems, collectionItems);
        } else {
            addRecentlyPlayed(recentlyPlayedPlayableItems, collectionItems);
            addPlayHistory(playHistoryTrackItems, collectionItems);
        }

        return collectionItems;
    }

    private void addRecentlyPlayed(List<RecentlyPlayedPlayableItem> recentlyPlayedPlayableItems, List<CollectionItem> collectionItems) {
        if (recentlyPlayedPlayableItems.size() > 0) {
            collectionItems.add(RecentlyPlayedBucketItem.create(recentlyPlayedPlayableItems));
        }
    }

    private void addPlayHistory(List<TrackItem> tracks, List<CollectionItem> collectionItems) {
        if (tracks.size() > 0) {
            collectionItems.add(PlayHistoryBucketItem.create(tracks));
        }
    }
}
