package com.soundcloud.android.playback.ui;

import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.java.collections.PropertySet;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class PlayQueueDataSource {

    private final PlayQueueManager playQueueManager;
    private final List<TrackPageData> fullQueue;

    @Inject
    public PlayQueueDataSource(PlayQueueManager playQueueManager) {
        this.playQueueManager = playQueueManager;
        this.fullQueue = createFullQueue();
    }

    public List<TrackPageData> getFullQueue() {
        return fullQueue;
    }

    public List<TrackPageData> getCurrentTrackAsQueue() {
        final TrackQueueItem trackQueueItem = (TrackQueueItem) playQueueManager.getCurrentPlayQueueItem();

        final TrackPageData adPageData = new TrackPageData(playQueueManager.getCurrentPosition(),
                trackQueueItem.getTrackUrn(),
                playQueueManager.getCollectionUrn(),
                trackQueueItem.getMetaData());
        return newArrayList(adPageData);
    }

    private List<TrackPageData> createFullQueue() {
        List<TrackPageData> trackPageDataCollection = new ArrayList<>(playQueueManager.getQueueSize());
        for (int i = 0; i < playQueueManager.getQueueSize(); i++) {
            final PlayQueueItem playQueueItem = playQueueManager.getPlayQueueItemAtPosition(i);
            if (playQueueItem.isTrack()) {
                final TrackQueueItem trackQueueItem = (TrackQueueItem) playQueueItem;
                final Urn trackUrn = trackQueueItem.getTrackUrn();
                final PropertySet metaData = trackQueueItem.getMetaData();
                final Urn collectionUrn = playQueueManager.getCollectionUrn();
                trackPageDataCollection.add(new TrackPageData(i, trackUrn, collectionUrn, metaData));
            }
        }
        return trackPageDataCollection;
    }

}
