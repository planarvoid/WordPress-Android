package com.soundcloud.android.playback;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UIEvent.PlayerInterface;

import javax.inject.Inject;

public class PlayerInteractionsTracker {

    private final EventTracker eventTracker;

    @Inject
    public PlayerInteractionsTracker(EventTracker eventTracker) {
        this.eventTracker = eventTracker;
    }

    public void clickForward(PlaybackActionSource playbackActionSource) {
        dispatchEvent(UIEvent.fromPlayerClickForward(toPlayerInterface(playbackActionSource)));
    }

    public void clickBackward(PlaybackActionSource playbackActionSource) {
        dispatchEvent(UIEvent.fromPlayerClickBackward(toPlayerInterface(playbackActionSource)));
    }

    public void swipeForward(PlaybackActionSource playbackActionSource) {
        dispatchEvent(UIEvent.fromPlayerSwipeForward(toPlayerInterface(playbackActionSource)));
    }

    public void swipeBackward(PlaybackActionSource playbackActionSource) {
        dispatchEvent(UIEvent.fromPlayerSwipeBackward(toPlayerInterface(playbackActionSource)));
    }

    private void dispatchEvent(UIEvent event) {
        eventTracker.trackClick(event);
    }

    private PlayerInterface toPlayerInterface(PlaybackActionSource playbackActionSource) {
        switch (playbackActionSource) {
            case FULL:
                return PlayerInterface.FULLSCREEN;
            case MINI:
                return PlayerInterface.MINI;
            case NOTIFICATION:
                return PlayerInterface.NOTIFICATION;
            case WIDGET:
                return PlayerInterface.WIDGET;
            default:
                return PlayerInterface.OTHER;
        }
    }
}
