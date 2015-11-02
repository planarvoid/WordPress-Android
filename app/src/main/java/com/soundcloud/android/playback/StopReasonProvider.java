package com.soundcloud.android.playback;

import com.soundcloud.android.events.PlaybackSessionEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StopReasonProvider {

    private final PlayQueueManager playQueueManager;
    private boolean pendingConcurrentPause;

    @Inject
    public StopReasonProvider(PlayQueueManager playQueueManager) {
        this.playQueueManager = playQueueManager;
    }

    public void setPendingConcurrentPause() {
        pendingConcurrentPause = true;
    }

    public int fromTransition(Player.StateTransition stateTransition) {
        if (stateTransition.isBuffering()) {
            return PlaybackSessionEvent.STOP_REASON_BUFFERING;
        } else {
            final int idleReason = getIdleReason(stateTransition);
            pendingConcurrentPause = false;
            return idleReason;
        }
    }

    private int getIdleReason(Player.StateTransition stateTransition) {
        if (stateTransition.getReason() == Player.Reason.PLAYBACK_COMPLETE) {
            return getTrackCompleteReason();
        } else if (stateTransition.wasError()) {
            return PlaybackSessionEvent.STOP_REASON_ERROR;
        } else if (pendingConcurrentPause) {
            return PlaybackSessionEvent.STOP_REASON_CONCURRENT_STREAMING;
        } else {
            return PlaybackSessionEvent.STOP_REASON_PAUSE;
        }
    }

    private int getTrackCompleteReason() {
        return playQueueManager.hasNextItem()
                ? PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED
                : PlaybackSessionEvent.STOP_REASON_END_OF_QUEUE;
    }
}
