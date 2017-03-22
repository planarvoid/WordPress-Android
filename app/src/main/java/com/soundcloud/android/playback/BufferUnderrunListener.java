package com.soundcloud.android.playback;

import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.rx.eventbus.EventBus;

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
                                  CurrentDateProvider dateProvider) {
        this.detector = detector;
        this.eventBus = eventBus;
        this.uninterruptedPlaytimeStorage = uninterruptedPlaytimeStorage;
        this.dateProvider = dateProvider;
    }

    public void onPlaystateChanged(PlaybackItem playbackItem,
                                   PlaybackStateTransition stateTransition,
                                   PlaybackProtocol playbackProtocol,
                                   PlayerType playerType,
                                   ConnectionType currentConnectionType) {
        Log.d(TAG, "PlaybackStateTransition: " + stateTransition);
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
                emitUninterruptedPlaytimeEvent(playbackItem,
                                               playbackProtocol,
                                               playerType,
                                               currentConnectionType,
                                               uninterruptedPlayTime,
                                               stateTransition.getFormat(),
                                               stateTransition.getBitrate());
                uninterruptedPlayTime = 0L;
            }
            enteringPlayingStateTime = null;
            uninterruptedPlaytimeStorage.setPlaytime(uninterruptedPlayTime, playerType);
        }
    }

    private long incrementPlaytime(long uninterruptedPlayTime) {
        return uninterruptedPlayTime + (dateProvider.getCurrentDate().getTime() - enteringPlayingStateTime.getTime());
    }

    private void emitUninterruptedPlaytimeEvent(PlaybackItem item,
                                                PlaybackProtocol playbackProtocol,
                                                PlayerType playerType,
                                                ConnectionType currentConnectionType,
                                                long uninterruptedPlayTime,
                                                String format,
                                                int bitrate) {
        PlaybackType playbackType = item.getPlaybackType();

        final PlaybackPerformanceEvent event = PlaybackPerformanceEvent.uninterruptedPlaytimeMs(playbackType)
                                                                       .metricValue(uninterruptedPlayTime)
                                                                       .protocol(playbackProtocol)
                                                                       .playerType(playerType)
                                                                       .connectionType(currentConnectionType)
                                                                       .cdnHost(item.toString())
                                                                       .format(format)
                                                                       .bitrate(bitrate)
                                                                       .build();
        Log.i(TAG, "Playa buffer underrun. " + event);
        eventBus.publish(EventQueue.PLAYBACK_PERFORMANCE, event);
    }

    // This should be removed when we discover why we are getting empty player types
    private void checkForEmptyPlayerType(PlaybackStateTransition stateTransition) {
        if (TextUtils.isEmpty(stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_PLAYER_TYPE))) {
            ErrorUtils.handleSilentException(TAG,
                                             new IllegalStateException("Buffer Underrun event with empty player type: " + stateTransition
                                                     .toString()));
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

        public boolean onStateTransitionEvent(PlaybackStateTransition transition) {
            if (isStartingPlaybackAfterSeek) {
                isStartingPlaybackAfterSeek = transition.isBuffering();
            }
            return !isStartingPlayback(transition) && transition.isBuffering();
        }

        private boolean isStartingPlayback(PlaybackStateTransition transition) {
            return isStartingPlaybackAfterSeek || transition.getProgress().getPosition() == 0;
        }

        public void onSeek() {
            isStartingPlaybackAfterSeek = true;
        }
    }
}
