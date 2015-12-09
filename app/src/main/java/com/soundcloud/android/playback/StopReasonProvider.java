package com.soundcloud.android.playback;

import com.soundcloud.android.events.PlaybackSessionEvent;

import javax.inject.Singleton;

@Singleton
public class StopReasonProvider {

    private final PlayQueueManager playQueueManager;

    public StopReasonProvider(PlayQueueManager playQueueManager) {
        this.playQueueManager = playQueueManager;
    }

    public int stopReasonFromTransition(Player.StateTransition stateTransition) {
        if (stateTransition.isBuffering()) {
            return PlaybackSessionEvent.STOP_REASON_BUFFERING;
        } else {
            if (stateTransition.getReason() == Player.Reason.TRACK_COMPLETE) {
                return playQueueManager.hasNextItem()
                        ? PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED
                        : PlaybackSessionEvent.STOP_REASON_END_OF_QUEUE;
            } else if (stateTransition.wasError()) {
                return PlaybackSessionEvent.STOP_REASON_ERROR;
            } else {
                return PlaybackSessionEvent.STOP_REASON_PAUSE;
            }
        }
    }
}
