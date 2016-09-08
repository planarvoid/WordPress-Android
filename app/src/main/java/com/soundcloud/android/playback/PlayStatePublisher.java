package com.soundcloud.android.playback;

import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlayStatePublisher {

    private final PlaySessionStateProvider playSessionStateProvider;

    private final PlaybackAnalyticsController analyticsController;
    private final PlayQueueAdvancer playQueueAdvancer;
    private final AdsController adsController;
    private final EventBus eventBus;

    @Inject
    public PlayStatePublisher(PlaySessionStateProvider playSessionStateProvider,
                              PlaybackAnalyticsController analyticsController,
                              PlayQueueAdvancer playQueueAdvancer, AdsController adsController,
                              EventBus eventBus) {
        this.playSessionStateProvider = playSessionStateProvider;
        this.analyticsController = analyticsController;
        this.playQueueAdvancer = playQueueAdvancer;
        this.adsController = adsController;
        this.eventBus = eventBus;
    }

    public void publish(PlaybackStateTransition stateTransition,
                        PlaybackItem currentPlaybackItem,
                        boolean publishAnalytics){

        PlayStateEvent playStateEvent = playSessionStateProvider.onPlayStateTransition(
                stateTransition,currentPlaybackItem.getDuration());

        if (publishAnalytics) {
            analyticsController.onStateTransition(currentPlaybackItem, playStateEvent);
        }
        adsController.onPlayStateChanged(playStateEvent);

        switch (playQueueAdvancer.onPlayStateChanged(playStateEvent)) {
            case QUEUE_COMPLETE:
                eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, PlayStateEvent.createPlayQueueCompleteEvent(playStateEvent));
                break;
            case ADVANCED:
                // we might be able to do nothing here. big change in behavior though. fall through for now
            case NO_OP:
                eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, playStateEvent);
                break;
        }

    }

}
