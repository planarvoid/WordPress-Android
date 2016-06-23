package com.soundcloud.android.playback;

import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlayStatePublisher {

    private final PlaybackAnalyticsController analyticsController;
    private final AdsController adsController;
    private final EventBus eventBus;

    @Inject
    public PlayStatePublisher(PlaybackAnalyticsController analyticsController, AdsController adsController, EventBus eventBus) {
        this.analyticsController = analyticsController;
        this.adsController = adsController;
        this.eventBus = eventBus;
    }

    public void publish(PlaybackStateTransition stateTransition, PlaybackItem currentItem){
        analyticsController.onStateTransition(currentItem, stateTransition);
        adsController.onPlayStateTransition(stateTransition);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, correctUnknownDuration(stateTransition, currentItem.getDuration()));
    }

    private static PlaybackStateTransition correctUnknownDuration(PlaybackStateTransition stateTransition, long apiDuration) {
        final PlaybackProgress progress = stateTransition.getProgress();
        if (!progress.isDurationValid()) {
            progress.setDuration(apiDuration);
        }
        return stateTransition;
    }

}
