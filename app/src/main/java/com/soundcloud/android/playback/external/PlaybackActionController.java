package com.soundcloud.android.playback.external;

import com.soundcloud.android.ServiceInitiator;
import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;

public class PlaybackActionController {

    private final PlaySessionController playSessionController;
    private final ServiceInitiator serviceInitiator;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final AdsController adsController;
    private final EventBus eventBus;

    @Inject
    public PlaybackActionController(PlaySessionController playSessionController,
                                    ServiceInitiator serviceInitiator,
                                    PlaySessionStateProvider playSessionStateProvider,
                                    AdsController adsController,
                                    EventBus eventBus) {
        this.playSessionController = playSessionController;
        this.serviceInitiator = serviceInitiator;
        this.playSessionStateProvider = playSessionStateProvider;
        this.adsController = adsController;
        this.eventBus = eventBus;
    }

    public void handleAction(String action, String source) {
        if (PlaybackAction.PLAY.equals(action)) {
            eventBus.publish(EventQueue.TRACKING, PlayControlEvent.play(source));
            playSessionController.play();
        } else if (PlaybackAction.PAUSE.equals(action)) {
            eventBus.publish(EventQueue.TRACKING, PlayControlEvent.pause(source));
            playSessionController.pause();
        } else if (PlaybackAction.PREVIOUS.equals(action)) {
            eventBus.publish(EventQueue.TRACKING, PlayControlEvent.previous(source));
            playSessionController.previousTrack();
        } else if (PlaybackAction.NEXT.equals(action)) {
            reconfigureAdIfBackgroundSkip(source);
            eventBus.publish(EventQueue.TRACKING, PlayControlEvent.skip(source));
            playSessionController.nextTrack();
        } else if (PlaybackAction.TOGGLE_PLAYBACK.equals(action)) {
            eventBus.publish(EventQueue.TRACKING, PlayControlEvent.toggle(source, playSessionStateProvider.isPlaying()));
            playSessionController.togglePlayback();
        } else if (PlaybackAction.CLOSE.equals(action)) {
            eventBus.publish(EventQueue.TRACKING, PlayControlEvent.close(source));
            serviceInitiator.stopPlaybackService();
        }
    }

    private void reconfigureAdIfBackgroundSkip(String source) {
        if (source.equals(PlayControlEvent.SOURCE_NOTIFICATION)
                || source.equals(PlayControlEvent.SOURCE_WIDGET)
                || source.equals(PlayControlEvent.SOURCE_REMOTE)) {
            adsController.reconfigureAdForNextTrack();
            adsController.publishAdDeliveryEventIfUpcoming();
        }
    }
}
