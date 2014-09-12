package com.soundcloud.android.playback.ui;

import com.google.common.collect.Lists;
import com.soundcloud.android.playback.service.PlayQueueManager;

import javax.inject.Inject;
import java.util.List;

class PlayQueueDataSwitcher {

    private final PlayQueueManager playQueueManager;
    private final List<TrackPageData> fullQueue;

    @Inject
    public PlayQueueDataSwitcher(PlayQueueManager playQueueManager) {
        this.playQueueManager = playQueueManager;
        this.fullQueue = createFullQueue();
    }

    public List<TrackPageData> getFullQueue() {
        return fullQueue;
    }

    public List<TrackPageData> getAdQueue() {
        final TrackPageData trackPageData = new TrackPageData(playQueueManager.getCurrentPosition(), playQueueManager.getCurrentTrackUrn(), playQueueManager.getAudioAd());
        return Lists.newArrayList(trackPageData);
    }

    private List<TrackPageData> createFullQueue() {
        List<TrackPageData> trackPageData = Lists.newArrayListWithExpectedSize(playQueueManager.getQueueSize());
        for (int i = 0; i < playQueueManager.getQueueSize(); i++){
            if (playQueueManager.isAudioAdAtPosition(i)){
                trackPageData.add(new TrackPageData(i, playQueueManager.getUrnAtPosition(i), playQueueManager.getAudioAd()));
            } else {
                trackPageData.add(new TrackPageData(i, playQueueManager.getUrnAtPosition(i)));
            }
        }
        return trackPageData;
    }

}
