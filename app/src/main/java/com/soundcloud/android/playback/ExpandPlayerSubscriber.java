package com.soundcloud.android.playback;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ErrorUtils;

import android.os.Handler;
import android.os.Message;

import javax.inject.Inject;
import java.util.List;

public class ExpandPlayerSubscriber extends DefaultSubscriber<List<Urn>> {
    public static final int EXPAND_DELAY_MILLIS = 100;
    private final EventBus eventBus;
    private final PlaybackToastHelper playbackToastHelper;

    private final Handler expandDelayHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayerOpen(UIEvent.METHOD_TRACK_PLAY));
        }
    };

    @Inject
    public ExpandPlayerSubscriber(EventBus eventBus, PlaybackToastHelper playbackToastHelper) {
        this.eventBus = eventBus;
        this.playbackToastHelper = playbackToastHelper;
    }

    @Override
    public void onNext(List<Urn> args) {
        expandDelayHandler.sendEmptyMessageDelayed(0, EXPAND_DELAY_MILLIS);
    }

    @Override
    public void onError(Throwable e) {
        if (!playbackToastHelper.showToastOnPlaybackError(e)) {
            ErrorUtils.handleSilentException("Unhandled exception when expanding a player", e);
        }
    }
}
