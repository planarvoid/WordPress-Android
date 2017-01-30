package com.soundcloud.android.playback.ui;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.rx.eventbus.EventBus;

class PageListener {

    protected final PlaySessionController playSessionController;
    protected final EventBus eventBus;

    PageListener(PlaySessionController playSessionController, EventBus eventBus) {
        this.playSessionController = playSessionController;
        this.eventBus = eventBus;
    }

    void onTogglePlay() {
        playSessionController.togglePlayback();
    }

    void onFooterTap() {
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
    }

    void onPlayerClose() {
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayerManually());
    }

    void onFooterTogglePlay() {
        playSessionController.togglePlayback();
    }

    void requestPlayerCollapse() {
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayerAutomatically());
    }
}
