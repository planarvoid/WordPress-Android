package com.soundcloud.android.playback;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;

import android.os.Handler;
import android.os.Message;

import javax.inject.Inject;

public class ExpandPlayerSubscriber extends DefaultSubscriber<PlaybackResult> {
    public static final int EXPAND_DELAY_MILLIS = 100;

    private final PlaybackToastHelper playbackToastHelper;
    private final Handler expandDelayHandler;

    private static class ExpandDelayHandler extends Handler {
        private EventBus eventBus;

        private ExpandDelayHandler(EventBus eventBus) {
            this.eventBus = eventBus;
        }

        @Override
        public void handleMessage(Message msg) {
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayerOpen(UIEvent.METHOD_TRACK_PLAY));
        }
    }

    @Inject
    public ExpandPlayerSubscriber(EventBus eventBus, PlaybackToastHelper playbackToastHelper) {
        this.expandDelayHandler = new ExpandDelayHandler(eventBus);
        this.playbackToastHelper = playbackToastHelper;
    }

    @Override
    public void onNext(PlaybackResult result) {
        if (result.isSuccess()) {
            expandPlayer();
        } else {
            playbackToastHelper.showToastOnPlaybackError(result.getErrorReason());
        }
    }

    protected boolean expandPlayer() {
        return expandDelayHandler.sendEmptyMessageDelayed(0, EXPAND_DELAY_MILLIS);
    }

}
