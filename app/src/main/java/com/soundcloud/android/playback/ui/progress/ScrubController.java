package com.soundcloud.android.playback.ui.progress;

import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.view.ListenableHorizontalScrollView;

import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;

import javax.inject.Inject;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

public class ScrubController {

    public static final int SCRUB_STATE_NONE = 0;
    public static final int SCRUB_STATE_SCRUBBING = 1;

    private static final int MSG_PERFORM_SEEK = 0;
    private static final int SEEK_DELAY = 250;

    private final Handler seekHandler;
    private final PlaybackOperations playbackOperations;
    private final PlaySessionController playSessionController;
    private final Set<OnScrubListener> listeners = new HashSet<OnScrubListener>();

    private ProgressHelper progressHelper;
    private Float pendingSeek;
    private int scrubState;
    private boolean dragging;

    public interface OnScrubListener {
        void scrubStateChanged(int newScrubState);
        void displayScrubPosition(float scrubPosition);
    }

    private void finishSeek(Float seekPercentage) {
        final long position = (long) (seekPercentage * playSessionController.getCurrentProgress().getDuration());
        playbackOperations.seek(position);
        setScrubState(SCRUB_STATE_NONE);
        pendingSeek = null;
    }

    ScrubController(ListenableHorizontalScrollView scrubView, PlaybackOperations playbackOperations, PlaySessionController playSessionController) {
        this.playbackOperations = playbackOperations;
        this.playSessionController = playSessionController;
        this.seekHandler = new SeekHandler(this);

        scrubView.setOnScrollListener(new ScrollListener());
        scrubView.setOnTouchListener(new TouchListener());
    }

    private void setScrubState(int newState) {
        scrubState = newState;

        for (OnScrubListener listener : listeners) {
            listener.scrubStateChanged(scrubState);
        }
    }

    public void setProgressHelper(ProgressHelper progressHelper) {
        this.progressHelper = progressHelper;
    }

    public void addScrubListener(OnScrubListener listener) {
        listeners.add(listener);
    }

    private class ScrollListener implements ListenableHorizontalScrollView.OnScrollListener {
        @Override
        public void onScroll(int left, int oldLeft) {
            if (isScrubbing() && progressHelper != null) {
                final float seekTo = Math.max(0, Math.min(1, progressHelper.getProgressFromPosition(left)));
                seekHandler.removeMessages(MSG_PERFORM_SEEK);
                seekHandler.sendMessageDelayed(seekHandler.obtainMessage(MSG_PERFORM_SEEK, seekTo), SEEK_DELAY);

                for (OnScrubListener listener : listeners) {
                    listener.displayScrubPosition(seekTo);
                }
            }
        }
    }

    private class TouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                dragging = true;
                setScrubState(SCRUB_STATE_SCRUBBING);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                dragging = false;
                if (!seekHandler.hasMessages(MSG_PERFORM_SEEK) && pendingSeek != null) {
                    finishSeek(pendingSeek);
                }
            }
            return false;
        }
    }

    private boolean isScrubbing() {
        return scrubState == SCRUB_STATE_SCRUBBING || seekHandler.hasMessages(MSG_PERFORM_SEEK);
    }

    private static class SeekHandler extends Handler {

        private final WeakReference<ScrubController> scrubControllerRef;

        private SeekHandler(ScrubController scrubController) {
            this.scrubControllerRef = new WeakReference<ScrubController>(scrubController);
        }

        @Override
        public void handleMessage(Message msg) {
            ScrubController scrubController = scrubControllerRef.get();
            if (scrubController != null){
                if (scrubController.dragging) {
                    scrubController.pendingSeek = (Float) msg.obj;
                } else {
                    scrubController.finishSeek((Float) msg.obj);
                }
            }
        }
    }

    public static class ScrubControllerFactory {

        private final PlaybackOperations playbackOperations;
        private final PlaySessionController playSessionController;

        @Inject
        public ScrubControllerFactory(PlaybackOperations playbackOperations, PlaySessionController playSessionController) {
            this.playbackOperations = playbackOperations;
            this.playSessionController = playSessionController;
        }

        public ScrubController create(ListenableHorizontalScrollView scrubView) {
            return new ScrubController(scrubView, playbackOperations, playSessionController);
        }

    }
}
