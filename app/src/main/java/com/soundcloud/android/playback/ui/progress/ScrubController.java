package com.soundcloud.android.playback.ui.progress;

import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.view.WaveformScrollView;

import android.os.Handler;

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
    private final PlaySessionController playSessionController;
    private final Set<OnScrubListener> listeners = new HashSet<>();


    private ProgressHelper progressHelper;
    private Float pendingSeek;
    private int scrubState;
    private boolean dragging;
    private long apiDuration;

    public boolean isDragging() {
        return dragging;
    }

    public void setPendingSeek(Float seekPos) {
        pendingSeek = seekPos;
    }

    public void setFullDuration(long duration) {
        this.apiDuration = duration;
    }

    public interface OnScrubListener {
        void scrubStateChanged(int newScrubState);
        void displayScrubPosition(float scrubPosition, float boundedScrubPosition);
    }

    void finishSeek(Float seekPercentage) {
        final long position = (long) (seekPercentage * apiDuration);
        playSessionController.seek(position);
        setScrubState(SCRUB_STATE_NONE);
        pendingSeek = null;
    }

    ScrubController(WaveformScrollView scrubView, PlaySessionController playSessionController,
                    SeekHandler.Factory seekHandlerFactory) {

        this.playSessionController = playSessionController;
        this.seekHandler = seekHandlerFactory.create(this);

        scrubView.setListener(new ScrollViewListener());
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

    private class ScrollViewListener implements WaveformScrollView.OnScrollListener {
        @Override
        public void onScroll(int left, int oldLeft) {
            if (isScrubbing() && progressHelper != null) {
                final float actualPosition = progressHelper.getProgressFromPosition(left);
                final float boundedPosition = Math.max(0, Math.min(1, actualPosition));
                seekHandler.removeMessages(MSG_PERFORM_SEEK);
                seekHandler.sendMessageDelayed(seekHandler.obtainMessage(MSG_PERFORM_SEEK, boundedPosition), SEEK_DELAY);

                for (OnScrubListener listener : listeners) {
                    listener.displayScrubPosition(actualPosition, boundedPosition);
                }
            }
        }

        @Override
        public void onPress() {
            dragging = true;
            setScrubState(SCRUB_STATE_SCRUBBING);
        }

        @Override
        public void onRelease() {
            dragging = false;
            if (!seekHandler.hasMessages(MSG_PERFORM_SEEK)) {
                if (pendingSeek == null) {
                    setScrubState(SCRUB_STATE_CANCELLED);
                } else {
                    finishSeek(pendingSeek);
                }
            }
        }
    }

    private boolean isScrubbing() {
        return scrubState == SCRUB_STATE_SCRUBBING || seekHandler.hasMessages(MSG_PERFORM_SEEK);
    }

    public static class Factory {

        private final PlaySessionController playSessionController;
        private final SeekHandler.Factory seekHandlerFactory;

        @Inject
        public Factory(PlaySessionController playSessionController,
                       SeekHandler.Factory seekHandlerFactory) {
            this.playSessionController = playSessionController;
            this.seekHandlerFactory = seekHandlerFactory;
        }

        public ScrubController create(WaveformScrollView scrubView) {
            return new ScrubController(scrubView, playSessionController, seekHandlerFactory);
        }

    }
}
