package com.soundcloud.android.playback;

import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.utils.NetworkConnectionHelper;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlayQueueAdvancer {

    private final PlayQueueManager playQueueManager;
    private final NetworkConnectionHelper connectionHelper;
    private final PlaySessionController playSessionController;
    private final AdsController adsController;
    private final PlaybackServiceController serviceController;
    private final CastConnectionHelper castConnectionHelper;

    public enum Result {
        NO_OP, ADVANCED, QUEUE_COMPLETE
    }

    @Inject
    public PlayQueueAdvancer(PlayQueueManager playQueueManager,
                             NetworkConnectionHelper connectionHelper,
                             PlaySessionController playSessionController,
                             AdsController adsController,
                             PlaybackServiceController serviceController,
                             CastConnectionHelper castConnectionHelper) {
        this.playQueueManager = playQueueManager;
        this.connectionHelper = connectionHelper;
        this.playSessionController = playSessionController;
        this.adsController = adsController;
        this.serviceController = serviceController;
        this.castConnectionHelper = castConnectionHelper;
    }

    public Result onPlayStateChanged(PlayStateEvent playStateEvent) {
        if (shouldAdvanceItems(playStateEvent)) {
            reconfigureUpcomingAd();

            if (playQueueManager.autoMoveToNextPlayableItem()) {
                if (!playStateEvent.playSessionIsActive()) {
                    playSessionController.playCurrent();
                }
                return Result.ADVANCED;
            } else {
                serviceController.stopPlaybackService();
                return Result.QUEUE_COMPLETE;
            }
        }
        return Result.NO_OP;
    }

    private boolean shouldAdvanceItems(PlayStateEvent playStateEvent) {
        return !castConnectionHelper.isCasting() &&
                playQueueManager.isCurrentItem(playStateEvent.getPlayingItemUrn())
                && playStateEvent.isPlayerIdle()
                && !playStateEvent.isPlayQueueComplete()
                && (playStateEvent.playbackEnded()
                || unrecoverableErrorDuringAutoplay(playStateEvent.getTransition()));
    }

    private void reconfigureUpcomingAd() {
        adsController.reconfigureAdForNextTrack();
        adsController.publishAdDeliveryEventIfUpcoming();
    }

    private boolean unrecoverableErrorDuringAutoplay(PlaybackStateTransition stateTransition) {
        final TrackSourceInfo currentTrackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        return stateTransition.wasError() && !stateTransition.wasGeneralFailure() &&
                currentTrackSourceInfo != null && !currentTrackSourceInfo.getIsUserTriggered()
                && connectionHelper.isNetworkConnected();
    }
}
