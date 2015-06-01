package com.soundcloud.android.playback.ui.progress;

import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.view.ListenableHorizontalScrollView;

import android.graphics.Rect;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

public class ScrubController {

    public static final int SCRUB_STATE_NONE = 0;
    public static final int SCRUB_STATE_SCRUBBING = 1;
    public static final int SCRUB_STATE_CANCELLED = 2;

    static final int MSG_PERFORM_SEEK = 0;
    static final int SEEK_DELAY = 250;

    private final Handler seekHandler;
    private final PlaybackOperations playbackOperations;
    private final Set<OnScrubListener> listeners = new HashSet<>();
    private final Rect scrubViewBounds = new Rect();

    private ProgressHelper progressHelper;
    private Float pendingSeek;
    private int scrubState;
    private boolean dragging;
    private long duration;

    public boolean isDragging() {
        return dragging;
    }

    public void setPendingSeek(Float seekPos) {
        pendingSeek = seekPos;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public interface OnScrubListener {
        void scrubStateChanged(int newScrubState);
        void displayScrubPosition(float scrubPosition);
    }

    void finishSeek(Float seekPercentage) {
        final long position = (long) (seekPercentage * duration);
        playbackOperations.seek(position);
        setScrubState(SCRUB_STATE_NONE);
        pendingSeek = null;
    }

    ScrubController(ListenableHorizontalScrollView scrubView, PlaybackOperations playbackOperations,
                    SeekHandler.Factory seekHandlerFactory) {

        this.playbackOperations = playbackOperations;
        this.seekHandler = seekHandlerFactory.create(this);

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
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL || isOutsideBounds(v, event)) {
                dragging = false;
                onRelease();
            }
            return false;
        }
    }

    private boolean isOutsideBounds(View v, MotionEvent event) {
        scrubViewBounds.set(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        return !scrubViewBounds.contains(v.getLeft() + (int) event.getX(), v.getTop() + (int) event.getY());
    }

    private void onRelease() {
        if (!seekHandler.hasMessages(MSG_PERFORM_SEEK)) {
            if (pendingSeek == null) {
                setScrubState(SCRUB_STATE_CANCELLED);
            } else {
                finishSeek(pendingSeek);
            }
        }
    }

    private boolean isScrubbing() {
        return scrubState == SCRUB_STATE_SCRUBBING || seekHandler.hasMessages(MSG_PERFORM_SEEK);
    }

    public static class Factory {

        private final PlaybackOperations playbackOperations;
        private final SeekHandler.Factory seekHandlerFactory;

        @Inject
        public Factory(PlaybackOperations playbackOperations,
                       SeekHandler.Factory seekHandlerFactory) {
            this.playbackOperations = playbackOperations;
            this.seekHandlerFactory = seekHandlerFactory;
        }

        public ScrubController create(ListenableHorizontalScrollView scrubView) {
            return new ScrubController(scrubView, playbackOperations, seekHandlerFactory);
        }

    }
}
