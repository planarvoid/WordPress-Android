package com.soundcloud.android.playback.ui;

import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
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
        final TrackPageData adPageData = new TrackPageData(playQueueManager.getCurrentPosition(),
                playQueueManager.getCurrentTrackUrn(),
                playQueueManager.getCollectionUrn(),
                playQueueManager.getCurrentMetaData());
        return newArrayList(adPageData);
    }

    private List<TrackPageData> createFullQueue() {
        List<TrackPageData> trackPageDataCollection = new ArrayList<>(playQueueManager.getQueueSize());
        for (int i = 0; i < playQueueManager.getQueueSize(); i++){
            final Urn trackUrn = playQueueManager.getUrnAtPosition(i);
            final PropertySet metaData = playQueueManager.getMetaDataAt(i);
            final Urn collectionUrn = playQueueManager.getCollectionUrn();
            trackPageDataCollection.add(new TrackPageData(i, trackUrn, collectionUrn, metaData));
        }
        return trackPageDataCollection;
    }

}
