package com.soundcloud.android.playback.ui;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.rx.eventbus.EventBus;

import javax.inject.Inject;

class TrackPageListener {

    private final PlaybackOperations playbackOperations;
    private final EventBus eventBus;

    @Inject
    public TrackPageListener(PlaybackOperations playbackOperations,
                             EventBus eventBus) {
        this.playbackOperations = playbackOperations;
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
        if (playbackOperations.isProgressWithinTrackChangeThreshold()) {
            playbackOperations.previousTrack();
        } else {
            playbackOperations.restartPlayback();
        }
    }

}
