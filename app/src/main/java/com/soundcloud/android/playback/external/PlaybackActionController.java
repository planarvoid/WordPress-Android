package com.soundcloud.android.playback.external;

import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.ads.PlayerAdsController;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.utils.Log;

import javax.inject.Inject;

public class PlaybackActionController {

    private static final String TAG = "PlaybackActionCtrl";

    private final PlaySessionController playSessionController;
    private final PlaybackServiceController serviceController;
    private final PlayerAdsController adsController;

    @Inject
    public PlaybackActionController(PlaySessionController playSessionController,
                                    PlaybackServiceController serviceController,
                                    PlayerAdsController adsController) {
        this.playSessionController = playSessionController;
        this.serviceController = serviceController;
        this.adsController = adsController;
    }

    public void handleAction(String action, String source) {
        Log.d(TAG, "Handling Playback action " + action + " from " + source);
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
            serviceController.stopPlaybackService();
        }
    }

    private void reconfigureAdIfBackgroundSkip(String source) {
        switch (source) {
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
