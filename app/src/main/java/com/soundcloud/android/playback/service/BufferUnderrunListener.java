package com.soundcloud.android.playback.service;

import com.soundcloud.android.crop.util.VisibleForTesting;
import com.soundcloud.android.events.BufferUnderrunEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.utils.Log;

import javax.inject.Inject;

public class BufferUnderrunListener implements Playa.PlayaListener {
    private static final String TAG = "BufferUnderrunListener";

    private final Detector detector;
    private final EventBus eventBus;

    @Inject
    public BufferUnderrunListener(Detector detector,
                                  EventBus eventBus) {
        this.detector = detector;
        this.eventBus = eventBus;
    }

    @Override
    public void onPlaystateChanged(Playa.StateTransition stateTransition) {
        Log.d(TAG, "StateTransition: " + stateTransition);
        if (detector.onStateTransitionEvent(stateTransition)) {
            final BufferUnderrunEvent event = new BufferUnderrunEvent(
                    stateTransition.getExtraAttribute(Playa.StateTransition.EXTRA_CONNECTION_TYPE),
                    stateTransition.getExtraAttribute(Playa.StateTransition.EXTRA_PLAYER_TYPE),
                    stateTransition.getExtraAttribute(Playa.StateTransition.EXTRA_NETWORK_AND_WAKE_LOCKS_ACTIVE));
            Log.i(TAG, "Playa buffer underrun. " + event);
            eventBus.publish(EventQueue.TRACKING, event);
        }
    }

    @Override
    public void onProgressEvent(long progress, long duration) {
        // No op
    }

    @Override
    public boolean requestAudioFocus() {
        return false;
    }

    public void onSeek() {
        Log.d(TAG, "onSeek");
        detector.onSeek();
    }

    @VisibleForTesting
    static class Detector {
        private boolean isStartingPlaybackAfterSeek = false;

        @Inject
        public Detector() {
            // For Dagger
        }

        public boolean onStateTransitionEvent(Playa.StateTransition transition) {
            if (isStartingPlaybackAfterSeek) {
                isStartingPlaybackAfterSeek = transition.isBuffering();
            }
            return !isStartingPlayback(transition) && transition.isBuffering();
        }

        private boolean isStartingPlayback(Playa.StateTransition transition) {
            return isStartingPlaybackAfterSeek || transition.getProgress().getPosition() == 0;
        }

        public void onSeek() {
            isStartingPlaybackAfterSeek = true;
        }
    }
}
