package com.soundcloud.android.playback.ui;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.java.collections.PropertySet;

import javax.inject.Inject;
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
        final TrackPageData adPageData = new TrackPageData(playQueueManager.getCurrentPosition(), playQueueManager.getCurrentTrackUrn(), playQueueManager.getCurrentMetaData());
        return Lists.newArrayList(adPageData);
    }

    private List<TrackPageData> createFullQueue() {
        List<TrackPageData> trackPageDataCollection = Lists.newArrayListWithExpectedSize(playQueueManager.getQueueSize());
        for (int i = 0; i < playQueueManager.getQueueSize(); i++){
            final Urn trackUrn = playQueueManager.getUrnAtPosition(i);
            final PropertySet metaData = playQueueManager.getMetaDataAt(i);
            trackPageDataCollection.add(new TrackPageData(i, trackUrn, metaData));
        }
        return trackPageDataCollection;
    }

}
