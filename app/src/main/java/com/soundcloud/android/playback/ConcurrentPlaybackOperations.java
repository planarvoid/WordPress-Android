package com.soundcloud.android.playback;

import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;

import javax.inject.Inject;

public class ConcurrentPlaybackOperations {

    private final StopReasonProvider stopReasonProvider;
    private final PlaySessionController playSessionController;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final PlaybackToastHelper playbackToastHelper;

    @Inject
    ConcurrentPlaybackOperations(StopReasonProvider stopReasonProvider,
                                 PlaySessionController playSessionController,
                                 PlaySessionStateProvider playSessionStateProvider,
                                 PlaybackToastHelper playbackToastHelper) {

        this.stopReasonProvider = stopReasonProvider;
        this.playSessionController = playSessionController;
        this.playSessionStateProvider = playSessionStateProvider;
        this.playbackToastHelper = playbackToastHelper;
    }

    public void pauseIfPlaying() {
        if (playSessionStateProvider.isPlaying()) {
            stopReasonProvider.setPendingConcurrentPause();
            playSessionController.pause();
            playbackToastHelper.showConcurrentStreamingStoppedToast();
        }
    }
}
