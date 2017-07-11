package com.soundcloud.android.playback.common;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.playback.Player;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import javax.inject.Inject;
import javax.inject.Named;

public class ProgressChangeHandler extends Handler {

    private Player.PlayerListener playerListener;

    @Inject
    public ProgressChangeHandler(@Named(ApplicationModule.MAIN_LOOPER) Looper looper) {
        super(looper);
    }

    public void setPlayerListener(Player.PlayerListener playerListener) {
        this.playerListener = playerListener;
    }

    @Override
    public void handleMessage(Message msg) {
        final ProgressChangeMessage message = (ProgressChangeMessage) msg.obj;
        playerListener.onProgressEvent(message.progress, message.duration);
    }

    public void report(long position, long duration) {
        sendMessage(obtainMessage(0, new ProgressChangeHandler.ProgressChangeMessage(position, duration)));
    }

    public static class ProgressChangeMessage {

        private final long progress;
        private final long duration;

        public ProgressChangeMessage(final long progress, final long duration) {
            this.progress = progress;
            this.duration = duration;
        }
    }
}
