package com.soundcloud.android.playback;

import com.google.common.annotations.VisibleForTesting;

import android.os.Handler;
import android.os.Message;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

public class ProgressReporter extends Handler {

    private static final int MESSAGE_ID = 0;

    private WeakReference<ProgressPusher> progressPusherReference;

    @Inject
    ProgressReporter() {
        // for dagger
    }

    @VisibleForTesting
    public void setProgressPusher(ProgressPusher progressPusher) {
        progressPusherReference = new WeakReference<>(progressPusher);
    }

    public void start(){
        sendEmptyMessage(MESSAGE_ID);
    }

    public void stop(){
        removeMessages(MESSAGE_ID);
    }

    @Override
    public void handleMessage(Message msg) {
        if (progressPusherReference != null) {
            final ProgressPusher progressPusher = progressPusherReference.get();
            if (progressPusher != null) {
                progressPusher.pushProgress();
                sendEmptyMessageDelayed(MESSAGE_ID, PlaybackConstants.PROGRESS_DELAY_MS);
            }
        }
    }

    public interface ProgressPusher {
        void pushProgress();
    }
}
