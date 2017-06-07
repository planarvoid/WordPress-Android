package com.soundcloud.android.playback;

import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;

import javax.inject.Inject;

public class ConcurrentPlaybackOperations {

    private final StopReasonProvider stopReasonProvider;
    private final PlaySessionController playSessionController;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final CastConnectionHelper castConnectionHelper;
    private final PlaybackFeedbackHelper playbackFeedbackHelper;

    @Inject
    ConcurrentPlaybackOperations(StopReasonProvider stopReasonProvider,
                                 PlaySessionController playSessionController,
                                 PlaySessionStateProvider playSessionStateProvider,
                                 CastConnectionHelper castConnectionHelper,
                                 PlaybackFeedbackHelper playbackFeedbackHelper) {
        this.stopReasonProvider = stopReasonProvider;
        this.playSessionController = playSessionController;
        this.playSessionStateProvider = playSessionStateProvider;
        this.castConnectionHelper = castConnectionHelper;
        this.playbackFeedbackHelper = playbackFeedbackHelper;
    }

    public void pauseIfPlaying() {
        if (!castConnectionHelper.isCasting() && playSessionStateProvider.isPlaying()) {
            stopReasonProvider.setPendingConcurrentPause();
            playSessionController.fadeAndPause();
            playbackFeedbackHelper.showConcurrentStreamingStoppedFeedback();
        }
    }
}
