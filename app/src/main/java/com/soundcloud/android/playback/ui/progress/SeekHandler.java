package com.soundcloud.android.playback.ui.progress;

import android.os.Handler;
import android.os.Message;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

class SeekHandler extends Handler {

    private final WeakReference<ScrubController> scrubControllerRef;

    SeekHandler(ScrubController scrubController) {
        this.scrubControllerRef = new WeakReference<>(scrubController);
    }

    @Override
    public void handleMessage(Message msg) {
        ScrubController scrubController = scrubControllerRef.get();
        if (scrubController != null){
            if (scrubController.isDragging()) {
                scrubController.setPendingSeek((Float) msg.obj);
            } else {
                scrubController.finishSeek((Float) msg.obj);
            }
        }
    }

    static class Factory {

        @Inject
        Factory() {
            // Required by Dagger.
        }

        SeekHandler create(ScrubController scrubController){
            return new SeekHandler(scrubController);
        }
    }
}
