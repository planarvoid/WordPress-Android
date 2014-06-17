package com.soundcloud.android.playback.ui;

import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.PlaybackOperations;

import javax.inject.Inject;

class TrackPageListener implements TrackPagePresenter.Listener {

    private final PlaybackOperations playbackOperations;
    private final EventBus eventBus;

    @Inject
    public TrackPageListener(PlaybackOperations playbackOperations, EventBus eventBus) {
        this.playbackOperations = playbackOperations;
        this.eventBus = eventBus;
    }

    @Override
    public void onTogglePlay() {
        playbackOperations.togglePlayback();
    }

    @Override
    public void onNext() {
        playbackOperations.nextTrack();
    }

    @Override
    public void onPrevious() {
        playbackOperations.previousTrack();
    }

    @Override
    public void onFooterTap() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forExpandPlayer());
    }

    @Override
    public void onPlayerClose() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forCollapsePlayer());
    }

}
