package com.soundcloud.android.playback.ui;

import com.google.common.collect.Lists;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;

import javax.inject.Inject;
import java.util.List;

class PlayQueueDataSwitcher {

    private final PlayQueueManager playQueueManager;
    private final List<TrackPageData> fullQueue;
    private final AdsOperations adsOperations;

    @Inject
    public PlayQueueDataSwitcher(PlayQueueManager playQueueManager, AdsOperations adsOperations) {
        this.playQueueManager = playQueueManager;
        this.adsOperations = adsOperations;
        this.fullQueue = createFullQueue();
    }

    public List<TrackPageData> getFullQueue() {
        return fullQueue;
    }

    public List<TrackPageData> getAdQueue() {
        final TrackPageData adPageData = TrackPageData.forAd(playQueueManager.getCurrentPosition(), playQueueManager.getCurrentTrackUrn(), playQueueManager.getCurrentMetaData());
        return Lists.newArrayList(adPageData);
    }

    private List<TrackPageData> createFullQueue() {
        List<TrackPageData> trackPageDataCollection = Lists.newArrayListWithExpectedSize(playQueueManager.getQueueSize());
        for (int i = 0; i < playQueueManager.getQueueSize(); i++){
            final TrackPageData trackPageData;
            final TrackUrn trackUrn = playQueueManager.getUrnAtPosition(i);
            final PropertySet metaData = playQueueManager.getMetaDataAt(i);
            if (adsOperations.isAudioAdAtPosition(i)){
                trackPageData = TrackPageData.forAd(i, trackUrn, metaData);
            } else {
                trackPageData = TrackPageData.forTrack(i, trackUrn, metaData);
            }
            trackPageDataCollection.add(trackPageData);
        }
        return trackPageDataCollection;
    }

}
