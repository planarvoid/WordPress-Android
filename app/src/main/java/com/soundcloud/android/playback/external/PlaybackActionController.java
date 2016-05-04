package com.soundcloud.android.playback.external;

import com.soundcloud.android.PlaybackServiceInitiator;
import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.playback.PlaySessionController;

import javax.inject.Inject;

public class PlaybackActionController {

    private final PlaySessionController playSessionController;
    private final PlaybackServiceInitiator serviceInitiator;
    private final AdsController adsController;

    @Inject
    public PlaybackActionController(PlaySessionController playSessionController,
                                    PlaybackServiceInitiator serviceInitiator,
                                    AdsController adsController) {
        this.playSessionController = playSessionController;
        this.serviceInitiator = serviceInitiator;
        this.adsController = adsController;
    }

    public void handleAction(String action, String source) {
        if (PlaybackAction.PLAY.equals(action)) {
            playSessionController.play();
        } else if (PlaybackAction.PAUSE.equals(action)) {
            playSessionController.pause();
        } else if (PlaybackAction.PREVIOUS.equals(action)) {
            playSessionController.previousTrack();
        } else if (PlaybackAction.NEXT.equals(action)) {
            reconfigureAdIfBackgroundSkip(source);
            playSessionController.nextTrack();
        } else if (PlaybackAction.TOGGLE_PLAYBACK.equals(action)) {
            playSessionController.togglePlayback();
        } else if (PlaybackAction.CLOSE.equals(action)) {
            serviceInitiator.stopPlaybackService();
        }
    }

    private void reconfigureAdIfBackgroundSkip(String source) {
        switch(source) {
            case PlaybackActionReceiver.SOURCE_REMOTE:
            case PlaybackActionReceiver.SOURCE_WIDGET:
                adsController.reconfigureAdForNextTrack();
                adsController.publishAdDeliveryEventIfUpcoming();
                break;
            default:
                break;
        }
    }
}
