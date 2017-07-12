package com.soundcloud.android.playback.common;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.Player;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import javax.inject.Inject;
import javax.inject.Named;

public class StateChangeHandler extends Handler {

    private Player.PlayerListener playerListener;

    @Inject
    public StateChangeHandler(@Named(ApplicationModule.MAIN_LOOPER) Looper looper) {
        super(looper);
    }

    public void setPlayerListener(Player.PlayerListener playerListener) {
        this.playerListener = playerListener;
    }

    @Override
    public void handleMessage(Message msg) {
        final StateChangeMessage message = (StateChangeMessage) msg.obj;
        playerListener.onPlaystateChanged(message.stateTransition);
    }

    public static class StateChangeMessage {
        public final PlaybackItem playbackItem;
        public final PlaybackStateTransition stateTransition;

        public StateChangeMessage(PlaybackItem item, PlaybackStateTransition transition) {
            this.playbackItem = item;
            this.stateTransition = transition;
        }
    }
}
