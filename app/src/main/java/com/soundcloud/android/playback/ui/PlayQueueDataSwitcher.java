package com.soundcloud.android.playback.ui;

import com.google.common.collect.Lists;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;

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
        List<TrackPageData> trackPageData = Lists.newArrayListWithExpectedSize(playQueueManager.getQueueSize());
        for (int i = 0; i < playQueueManager.getQueueSize(); i++){
            if (adsOperations.isAudioAdAtPosition(i)){
                trackPageData.add(TrackPageData.forAd(i, playQueueManager.getUrnAtPosition(i), playQueueManager.getMetaDataAt(i)));
            } else {
                trackPageData.add(TrackPageData.forTrack(i, playQueueManager.getUrnAtPosition(i)));
            }
        }
        return trackPageData;
    }

}
