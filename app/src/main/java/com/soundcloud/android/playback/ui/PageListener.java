package com.soundcloud.android.playback.ui;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.rx.eventbus.EventBus;

public class PageListener {

    private final PlaySessionStateProvider playSessionStateProvider;
    protected final PlaySessionController playSessionController;
    protected final EventBus eventBus;

    public PageListener(PlaySessionController playSessionController,
                        PlaySessionStateProvider playSessionStateProvider,
                        EventBus eventBus) {
        this.playSessionController = playSessionController;
        this.playSessionStateProvider = playSessionStateProvider;
        this.eventBus = eventBus;
    }

    public void onTogglePlay() {
        playSessionController.togglePlayback();
        trackTogglePlay(PlayControlEvent.SOURCE_FULL_PLAYER);
    }

    public void onFooterTap() {
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayerOpen(UIEvent.METHOD_TAP_FOOTER));
    }

    public void onPlayerClose() {
        requestPlayerCollapse();
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayerClose(UIEvent.METHOD_HIDE_BUTTON));
    }

    public void onFooterTogglePlay() {
        playSessionController.togglePlayback();
        trackTogglePlay(PlayControlEvent.SOURCE_FOOTER_PLAYER);
    }

    private void trackTogglePlay(String location) {
        if (playSessionStateProvider.isPlaying()) {
            eventBus.publish(EventQueue.TRACKING, PlayControlEvent.pause(location));
        } else {
            eventBus.publish(EventQueue.TRACKING, PlayControlEvent.play(location));
        }
    }

    protected void requestPlayerCollapse() {
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayer());
    }
}
