package com.soundcloud.android.playback.ui;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static com.soundcloud.java.collections.Lists.newArrayList;

class PlayQueueDataSource {

    private final PlayQueueManager playQueueManager;
    private final List<PlayerPageData> fullQueue;

    @Inject
    public PlayQueueDataSource(PlayQueueManager playQueueManager) {
        this.playQueueManager = playQueueManager;
        this.fullQueue = createFullQueue();
    }

    public List<PlayerPageData> getFullQueue() {
        return fullQueue;
    }

    public List<PlayerPageData> getCurrentItemAsQueue() {
        final PlayQueueItem playQueueItem = playQueueManager.getCurrentPlayQueueItem();
        return newArrayList(transformPlayQueueItem(playQueueItem, playQueueManager.getCurrentPosition()));
    }

    private List<PlayerPageData> createFullQueue() {
        List<PlayerPageData> playerPageDataCollection = new ArrayList<>(playQueueManager.getQueueSize());
        for (int i = 0; i < playQueueManager.getQueueSize(); i++) {
            final PlayQueueItem playQueueItem = playQueueManager.getPlayQueueItemAtPosition(i);
            playerPageDataCollection.add(transformPlayQueueItem(playQueueItem, i));
        }
        return playerPageDataCollection;
    }

    private PlayerPageData transformPlayQueueItem(PlayQueueItem playQueueItem, int position) {
        if (playQueueItem.isTrack()) {
            final Urn collectionUrn = playQueueManager.getCollectionUrn();
            return new TrackPageData(position, playQueueItem.getUrn(), collectionUrn, playQueueItem.getMetaData());
        } else {
            return new VideoPageData(position, playQueueItem.getMetaData());
        }
    }
}
