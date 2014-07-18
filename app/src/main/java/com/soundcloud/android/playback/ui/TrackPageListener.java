package com.soundcloud.android.playback.ui;

import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaybackOperations;

import javax.inject.Inject;

class TrackPageListener {

    private final PlaybackOperations playbackOperations;
    private final PlaySessionController playSessionController;
    private final EventBus eventBus;

    @Inject
    public TrackPageListener(PlaybackOperations playbackOperations,
                             PlaySessionController playSessionController,
                             EventBus eventBus) {
        this.playbackOperations = playbackOperations;
        this.playSessionController = playSessionController;
        this.eventBus = eventBus;
    }

    public void onTogglePlay() {
        playbackOperations.togglePlayback();
    }

    public void onNext() {
        playbackOperations.nextTrack();
    }

    public void onPrevious() {
        previousTrackOnInitialSecondsOfProgress();
    }

    public void onFooterTap() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forExpandPlayer());
    }

    public void onPlayerClose() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forCollapsePlayer());
    }

    private void previousTrackOnInitialSecondsOfProgress() {
        if (playSessionController.isProgressWithinTrackChangeThreshold()) {
            playbackOperations.previousTrack();
        } else {
            playbackOperations.restartPlayback();
        }
    }

}
