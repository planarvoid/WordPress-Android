package com.soundcloud.android.playback.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.playback.PlayerDeviceCompatibility;
import com.soundcloud.android.playback.service.Playa.PlayaListener;
import com.soundcloud.android.playback.service.Playa.PlayaState;
import com.soundcloud.android.playback.service.Playa.Reason;
import com.soundcloud.android.playback.service.Playa.StateTransition;
import com.soundcloud.android.playback.service.mediaplayer.MediaPlayerAdapter;
import com.soundcloud.android.playback.service.skippy.SkippyAdapter;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.propeller.PropertySet;

import android.content.Context;

import javax.inject.Inject;

//Not a hater
public class StreamPlaya implements PlayaListener {

    public static final String TAG = "StreamPlaya";

    @VisibleForTesting
    static boolean skippyFailedToInitialize;

    private final MediaPlayerAdapter mediaPlayaDelegate;
    private final SkippyAdapter skippyPlayaDelegate;
    private final BufferingPlaya bufferingPlayaDelegate;
    private final PlayerSwitcherInfo playerSwitcherInfo;
    private final OfflinePlaybackOperations offlinePlaybackOperations;
    private final NetworkConnectionHelper networkConnectionHelper;

    private Playa currentPlaya;
    private PlayaListener playaListener;

    // store start info so we can fallback and retry after Skippy failures
    private PropertySet lastTrackPlayed;
    private StateTransition lastStateTransition = StateTransition.DEFAULT;

    @Inject
    public StreamPlaya(Context context, MediaPlayerAdapter mediaPlayerAdapter,
                       SkippyAdapter skippyAdapter, BufferingPlaya bufferingPlaya, PlayerSwitcherInfo playerSwitcherInfo, OfflinePlaybackOperations offlinePlaybackOperations,
                       NetworkConnectionHelper networkConnectionHelper) {

        mediaPlayaDelegate = mediaPlayerAdapter;
        skippyPlayaDelegate = skippyAdapter;
        bufferingPlayaDelegate = bufferingPlaya;
        this.playerSwitcherInfo = playerSwitcherInfo;
        this.offlinePlaybackOperations = offlinePlaybackOperations;
        this.networkConnectionHelper = networkConnectionHelper;
        currentPlaya = bufferingPlayaDelegate;

        if (!skippyFailedToInitialize) {
            skippyFailedToInitialize = !skippyPlayaDelegate.init(context);
        }
    }

    /**
     * state storage. should be gone in player v2 *
     */

    @Deprecated
    public StateTransition getLastStateTransition() {
        return lastStateTransition;
    }

    public PlayaState getState() {
        return lastStateTransition.getNewState();
    }

    @Deprecated
    public boolean isPlaying() {
        return lastStateTransition.isPlaying();
    }

    @Deprecated
    public boolean isPlayerPlaying() {
        return lastStateTransition.isPlayerPlaying();
    }

    @Deprecated
    public boolean isBuffering() {
        return lastStateTransition.isBuffering();
    }

    @Deprecated
    public boolean playbackHasPaused() {
        return lastStateTransition.isPaused();
    }

    public void play(PropertySet track) {
        final boolean offline = isAvailableOffline(track);
        prepareForPlay(track, offline);

        if (offline) {
            currentPlaya.playOffline(track, 0);
        } else {
            currentPlaya.play(track);
        }
    }

    public void play(PropertySet track, long fromPos) {
        final boolean offline = isAvailableOffline(track);
        prepareForPlay(track, offline);

        if (offline) {
            currentPlaya.playOffline(track, fromPos);
        } else {
            currentPlaya.play(track, fromPos);
        }
    }

    public void playUninterrupted(PropertySet track) {
        final boolean offline = isAvailableOffline(track);
        prepareForPlay(track, offline);

        currentPlaya.playUninterrupted(track);
    }

    private void prepareForPlay(PropertySet track, boolean isAvailableOffline) {
        lastTrackPlayed = track;
        configureNextPlayaToUse(isAvailableOffline);
    }

    public boolean resume() {
        return currentPlaya.resume();
    }

    public void pause() {
        currentPlaya.pause();
    }

    public long seek(long ms, boolean performSeek) {
        return currentPlaya.seek(ms, performSeek);
    }

    public long getProgress() {
        return currentPlaya.getProgress();
    }

    public void setVolume(float v) {
        currentPlaya.setVolume(v);
    }

    public void stop() {
        currentPlaya.stop();
    }

    public boolean isSeekable() {
        return currentPlaya.isSeekable();
    }

    public void destroy() {
        // call stop first as it will save the queue/position
        mediaPlayaDelegate.destroy();
        if (!skippyFailedToInitialize) {
            skippyPlayaDelegate.destroy();
        }
    }

    public void setListener(PlayaListener playaListener) {
        this.playaListener = playaListener;
        if (currentPlaya != null) {
            currentPlaya.setListener(playaListener);
        }
    }

    @Override
    public void onPlaystateChanged(StateTransition stateTransition) {
        if (shouldFallbackToMediaPlayer(stateTransition)) {
            final long progress = skippyPlayaDelegate.getProgress();
            configureNextPlayaToUse(mediaPlayaDelegate);
            mediaPlayaDelegate.play(lastTrackPlayed, progress);
        } else {
            Preconditions.checkNotNull(playaListener, "Stream Player Listener is unexpectedly null when passing state");
            lastStateTransition = stateTransition;
            playaListener.onPlaystateChanged(stateTransition);
        }
    }

    private boolean shouldFallbackToMediaPlayer(StateTransition stateTransition) {
        return isUsingSkippyPlaya() && stateTransition.wasGeneralFailure() && networkConnectionHelper.isNetworkConnected();
    }

    @Override
    public void onProgressEvent(long progress, long duration) {
        playaListener.onProgressEvent(progress, duration);
    }

    @Override
    public boolean requestAudioFocus() {
        Preconditions.checkNotNull(playaListener, "Stream Player Listener is unexpectedly null when requesting audio focus");
        return playaListener.requestAudioFocus();
    }

    public void startBufferingMode(Urn trackUrn) {
        Playa lastPlaya = currentPlaya;
        currentPlaya = bufferingPlayaDelegate;

        lastStateTransition = new StateTransition(PlayaState.BUFFERING, Reason.NONE, trackUrn);
        onPlaystateChanged(lastStateTransition);

        if (lastPlaya != null) {
            lastPlaya.setListener(null);
            lastPlaya.stopForTrackTransition();
        }
    }

    private void configureNextPlayaToUse(boolean offlinePlayback) {
        configureNextPlayaToUse(getNextPlaya(offlinePlayback));
    }

    private void configureNextPlayaToUse(Playa nextPlaya) {
        Log.i(TAG, "Configuring next player to use : " + nextPlaya);

        if (currentPlaya != null && currentPlaya != nextPlaya) {
            currentPlaya.stopForTrackTransition();
        }

        currentPlaya = nextPlaya;
        currentPlaya.setListener(this);
    }

    private Playa getNextPlaya(boolean forOfflinePlayback) {
        //there is no offline playback for media player so try to force skippy
        if (!skippyFailedToInitialize && forOfflinePlayback) {
            return skippyPlayaDelegate;
        }

        if (skippyFailedToInitialize || playerSwitcherInfo.shouldForceMediaPlayer()) {
            return mediaPlayaDelegate;
        }

        return skippyPlayaDelegate;
    }

    private boolean isAvailableOffline(PropertySet track) {
        return !skippyFailedToInitialize && offlinePlaybackOperations.shouldPlayOffline(track);
    }

    private boolean isUsingSkippyPlaya() {
        return currentPlaya == skippyPlayaDelegate;
    }

    public static class PlayerSwitcherInfo {

        @Inject
        public PlayerSwitcherInfo() {
            // dagger
        }

        public boolean shouldForceMediaPlayer() {
            return PlayerDeviceCompatibility.shouldForceMediaPlayer();
        }
    }
}
