package com.soundcloud.android.playback.ui;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.rx.eventbus.EventBus;

public class PageListener {

    protected final PlaySessionController playSessionController;
    protected final EventBus eventBus;

    public PageListener(PlaySessionController playSessionController,
                        EventBus eventBus) {
        this.playSessionController = playSessionController;
        this.eventBus = eventBus;
    }

    public void onTogglePlay() {
        playSessionController.togglePlayback();
    }

    public void onFooterTap() {
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
    }

    public void onPlayerClose() {
        requestPlayerCollapse();
    }

    public void onFooterTogglePlay() {
        playSessionController.togglePlayback();
    }

    protected void requestPlayerCollapse() {
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayer());
    }
}
