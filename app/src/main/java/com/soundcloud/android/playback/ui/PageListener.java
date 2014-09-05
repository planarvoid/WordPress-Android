package com.soundcloud.android.playback.ui;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.rx.eventbus.EventBus;

public class PageListener {

    private final PlaySessionStateProvider playSessionStateProvider;
    protected final PlaybackOperations playbackOperations;
    protected final EventBus eventBus;

    public PageListener(PlaybackOperations playbackOperations,
                        PlaySessionStateProvider playSessionStateProvider,
                        EventBus eventBus) {
        this.playbackOperations = playbackOperations;
        this.playSessionStateProvider = playSessionStateProvider;
        this.eventBus = eventBus;
    }

    public void onTogglePlay() {
        playbackOperations.togglePlayback();
        trackTogglePlay(PlayControlEvent.SOURCE_FULL_PLAYER);
    }

    public void onFooterTap() {
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
        eventBus.publish(EventQueue.UI, UIEvent.fromPlayerOpen(UIEvent.METHOD_TAP_FOOTER));
    }

    public void onPlayerClose() {
        requestPlayerCollapse();
        eventBus.publish(EventQueue.UI, UIEvent.fromPlayerClose(UIEvent.METHOD_HIDE_BUTTON));
    }

    public void onFooterTogglePlay() {
        playbackOperations.togglePlayback();
        trackTogglePlay(PlayControlEvent.SOURCE_FOOTER_PLAYER);
    }

    private void trackTogglePlay(String location) {
        if (playSessionStateProvider.isPlaying()) {
            eventBus.publish(EventQueue.PLAY_CONTROL, PlayControlEvent.pause(location));
        } else {
            eventBus.publish(EventQueue.PLAY_CONTROL, PlayControlEvent.play(location));
        }
    }

    protected void requestPlayerCollapse() {
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayer());
    }
}
