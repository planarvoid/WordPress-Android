package com.soundcloud.android.playback;

import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;

import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import javax.inject.Inject;
import java.util.Date;

public class BufferUnderrunListener {
    private static final String TAG = "BufferUnderrunListener";

    private final Detector detector;
    private final EventBus eventBus;
    private final DateProvider dateProvider;
    private final UninterruptedPlaytimeStorage uninterruptedPlaytimeStorage;
    private Date enteringPlayingStateTime;

    @Inject
    public BufferUnderrunListener(Detector detector,
                                  EventBus eventBus,
                                  UninterruptedPlaytimeStorage uninterruptedPlaytimeStorage,
                                  DateProvider dateProvider) {
        this.detector = detector;
        this.eventBus = eventBus;
        this.uninterruptedPlaytimeStorage = uninterruptedPlaytimeStorage;
        this.dateProvider = dateProvider;
    }

    public void onPlaystateChanged(Playa.StateTransition stateTransition,
                                   PlaybackProtocol playbackProtocol,
                                   PlayerType playerType,
                                   ConnectionType currentConnectionType) {
        Log.d(TAG, "StateTransition: " + stateTransition);
        boolean isBufferUnderrun = detector.onStateTransitionEvent(stateTransition);
        if (stateTransition.isPlayerPlaying()) {
            if (enteringPlayingStateTime == null) {
                enteringPlayingStateTime = dateProvider.getCurrentDate();
            }
        } else if (enteringPlayingStateTime != null) {
            long uninterruptedPlayTime = uninterruptedPlaytimeStorage.getPlayTime(playerType);
            uninterruptedPlayTime = incrementPlaytime(uninterruptedPlayTime);
            if (isBufferUnderrun) {
                checkForEmptyPlayerType(stateTransition);
                emitUninterruptedPlaytimeEvent(stateTransition.getTrackUrn(), playbackProtocol, playerType, currentConnectionType, uninterruptedPlayTime);
                uninterruptedPlayTime = 0L;
            }
            enteringPlayingStateTime = null;
            uninterruptedPlaytimeStorage.setPlaytime(uninterruptedPlayTime, playerType);
        }
    }

    private long incrementPlaytime(long uninterruptedPlayTime) {
        return uninterruptedPlayTime + (dateProvider.getCurrentDate().getTime() - enteringPlayingStateTime.getTime());
    }

    private void emitUninterruptedPlaytimeEvent(Urn track, PlaybackProtocol playbackProtocol, PlayerType playerType, ConnectionType currentConnectionType, long uninterruptedPlayTime) {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.uninterruptedPlaytimeMs(uninterruptedPlayTime,
                playbackProtocol, playerType, currentConnectionType, track.toString());
        Log.i(TAG, "Playa buffer underrun. " + event);
        eventBus.publish(EventQueue.PLAYBACK_PERFORMANCE, event);
    }

    // This should be removed when we discover why we are getting empty player types
    private void checkForEmptyPlayerType(Playa.StateTransition stateTransition) {
        if (TextUtils.isEmpty(stateTransition.getExtraAttribute(Playa.StateTransition.EXTRA_PLAYER_TYPE))) {
            ErrorUtils.handleSilentException(TAG,
                    new IllegalStateException("Buffer Underrun event with empty player type: " + stateTransition.toString()));
        }
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
