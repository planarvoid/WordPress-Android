package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.rx.eventbus.EventBus;

import javax.inject.Inject;

class TrackPageListener {
    private final PlaybackOperations playbackOperations;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final SoundAssociationOperations associationOperations;
    private final PlayQueueManager playQueueManager;
    private final EventBus eventBus;

    @Inject
    public TrackPageListener(PlaybackOperations playbackOperations, PlaySessionStateProvider playSessionStateProvider,
                             SoundAssociationOperations associationOperations,
                             PlayQueueManager playQueueManager, EventBus eventBus) {
        this.playbackOperations = playbackOperations;
        this.playSessionStateProvider = playSessionStateProvider;
        this.associationOperations = associationOperations;
        this.playQueueManager = playQueueManager;
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

    public void onToggleLike(boolean isLike) {
        fireAndForget(associationOperations.toggleLike(playQueueManager.getCurrentTrackUrn(), isLike));
    }

    public void onToggleRepost(boolean isRepost) {
        fireAndForget(associationOperations.toggleRepost(playQueueManager.getCurrentTrackUrn(), isRepost));
    }

    private void previousTrackOnInitialSecondsOfProgress() {
        playbackOperations.previousTrack();
    }

}
