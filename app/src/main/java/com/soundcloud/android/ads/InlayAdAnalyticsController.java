package com.soundcloud.android.ads;

import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playback.AdSessionAnalyticsDispatcher;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.TrackSourceInfo;

import javax.inject.Inject;

public class InlayAdAnalyticsController {

    private final AdSessionAnalyticsDispatcher adAnalyticsDispatcher;

    private PlaybackProgress lastProgressCheckpoint = PlaybackProgress.empty();
    private PlayStateEvent playStateEvent = PlayStateEvent.DEFAULT;

    @Inject
    InlayAdAnalyticsController(AdSessionAnalyticsDispatcher adAnalyticsController) {
        this.adAnalyticsDispatcher = adAnalyticsController;
    }

    void onStateTransition(Screen origin, boolean userTriggered, PlayableAdData adData, PlayStateEvent playState) {
        final boolean isNewItem = !playState.getPlayingItemUrn().equals(playStateEvent.getPlayingItemUrn());

        adAnalyticsDispatcher.setAdMetadata(adData, new TrackSourceInfo(origin.get(), userTriggered));
        if (playState.isPlayerPlaying()) {
            adAnalyticsDispatcher.onPlayTransition(playState, isNewItem);
        } else {
            adAnalyticsDispatcher.onStopTransition(playState, isNewItem);
        }

        playStateEvent = playState;
    }


    public void onProgressEvent(PlayableAdData adData, PlaybackProgressEvent playbackProgress) {
        if (playStateEvent.getPlayingItemUrn().equals(adData.getAdUrn())) {
            final PlaybackProgress currentProgress = playbackProgress.getPlaybackProgress();
            final long earliestPositionForCheckpoint = lastProgressCheckpoint.getPosition() + AdSessionAnalyticsDispatcher.CHECKPOINT_INTERVAL;

            if (currentProgress.getPosition() >= earliestPositionForCheckpoint) {
                adAnalyticsDispatcher.onProgressCheckpoint(playStateEvent, playbackProgress);
                lastProgressCheckpoint = currentProgress;
            }

            adAnalyticsDispatcher.onProgressEvent(playbackProgress);
        }
    }
}
