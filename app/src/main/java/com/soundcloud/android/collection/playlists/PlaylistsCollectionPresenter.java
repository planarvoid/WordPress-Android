package com.soundcloud.android.collection.playlists;

import com.soundcloud.android.collection.CollectionAdapter;
import com.soundcloud.android.collection.CollectionItem;
import com.soundcloud.android.collection.CollectionOperations;
import com.soundcloud.android.collection.CollectionOptionsStorage;
import com.soundcloud.android.collection.CollectionPlaylistOptionsPresenter;
import com.soundcloud.android.collection.CollectionPresenter;
import com.soundcloud.android.collection.MyCollection;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.res.Resources;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class PlaylistsCollectionPresenter extends CollectionPresenter {

    @Inject
    public PlaylistsCollectionPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                                        CollectionOperations collectionOperations,
                                        CollectionOptionsStorage collectionOptionsStorage,
                                        CollectionAdapter adapter,
                                        CollectionPlaylistOptionsPresenter optionsPresenter,
                                        Resources resources,
                                        EventBus eventBus) {
        super(swipeRefreshAttacher, collectionOperations, collectionOptionsStorage, adapter, optionsPresenter,
              resources, eventBus);
    }

    @Override
    protected List<CollectionItem> buildCollectionItems(MyCollection myCollection) {
        List<PlaylistItem> playlistItems = myCollection.getPlaylistItems();
        List<CollectionItem> collectionItems = new ArrayList<>(playlistItems.size() + 2);
        collectionItems.addAll(playlistCollectionItems(playlistItems, true));
        return collectionItems;
    }

    @Override
    protected boolean showOnboarding() {
        return false;
    }
}

