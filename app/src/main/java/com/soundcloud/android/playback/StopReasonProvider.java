package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.StopReasonProvider.StopReason.*;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StopReasonProvider {

    public enum StopReason {
        STOP_REASON_PAUSE("pause"),
        STOP_REASON_BUFFERING("buffer_underrun"),
        STOP_REASON_SKIP("skip"),
        STOP_REASON_TRACK_FINISHED("track_finished"),
        STOP_REASON_END_OF_QUEUE("end_of_content"),
        STOP_REASON_NEW_QUEUE("context_change"),
        STOP_REASON_ERROR("playback_error"),
        STOP_REASON_CONCURRENT_STREAMING("concurrent_streaming");
        private final String key;

        StopReason(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key;
        }
    }

    private final PlayQueueManager playQueueManager;
    private boolean pendingConcurrentPause;

    @Inject
    public StopReasonProvider(PlayQueueManager playQueueManager) {
        this.playQueueManager = playQueueManager;
    }

    public void setPendingConcurrentPause() {
        pendingConcurrentPause = true;
    }

    public StopReason fromTransition(PlaybackStateTransition stateTransition) {
        if (stateTransition.isBuffering()) {
            return STOP_REASON_BUFFERING;
        } else {
            final StopReason idleReason = getIdleReason(stateTransition);
            pendingConcurrentPause = false;
            return idleReason;
        }
    }

    private StopReason getIdleReason(PlaybackStateTransition stateTransition) {
        if (stateTransition.getReason() == PlayStateReason.PLAYBACK_COMPLETE) {
            return getTrackCompleteReason();
        } else if (stateTransition.wasError()) {
            return STOP_REASON_ERROR;
        } else if (pendingConcurrentPause) {
            return STOP_REASON_CONCURRENT_STREAMING;
        } else {
            return STOP_REASON_PAUSE;
        }
    }

    private StopReason getTrackCompleteReason() {
        return playQueueManager.hasNextItem()
               ? STOP_REASON_TRACK_FINISHED
               : STOP_REASON_END_OF_QUEUE;
    }
}
