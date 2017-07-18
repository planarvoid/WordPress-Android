package com.soundcloud.android.playback.ui;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaybackActionSource;
import com.soundcloud.android.playback.PlayerInteractionsTracker;
import com.soundcloud.rx.eventbus.EventBus;

class PageListener {

    protected final PlaySessionController playSessionController;
    protected final EventBus eventBus;
    private final PlayerInteractionsTracker playerInteractionsTracker;

    PageListener(PlaySessionController playSessionController,
                 EventBus eventBus,
                 PlayerInteractionsTracker playerInteractionsTracker) {
        this.playSessionController = playSessionController;
        this.eventBus = eventBus;
        this.playerInteractionsTracker = playerInteractionsTracker;
    }

    void onTogglePlay() {
        trackPlayerInteraction(PlaybackActionSource.FULL);
        playSessionController.togglePlayback();
    }

    void onFooterTap() {
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayerClickOpen(true));
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayerManually());
    }

    void onPlayerClose() {
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayerClickClose(true));
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayerManually());
    }

    void onFooterTogglePlay() {
        trackPlayerInteraction(PlaybackActionSource.MINI);
        playSessionController.togglePlayback();
    }

    private void trackPlayerInteraction(PlaybackActionSource source) {
        if (playSessionController.isPlaying()) {
            playerInteractionsTracker.pause(source);
        } else {
            playerInteractionsTracker.play(source);
        }
    }

    void requestPlayerCollapse() {
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayerAutomatically());
    }
}
