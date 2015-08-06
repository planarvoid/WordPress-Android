package com.soundcloud.android.playback;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.playback.Playa.PlayaListener;
import com.soundcloud.android.playback.mediaplayer.MediaPlayerAdapter;
import com.soundcloud.android.playback.skippy.SkippyAdapter;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.PropertySet;

import android.content.Context;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;

//Not a hater
class StreamPlaya implements PlayaListener {

    public static final String TAG = "StreamPlaya";

    @VisibleForTesting
    static boolean skippyFailedToInitialize;

    private final MediaPlayerAdapter mediaPlayaDelegate;
    private final SkippyAdapter skippyPlayaDelegate;
    private final BufferingPlaya bufferingPlayaDelegate;
    private final OfflinePlaybackOperations offlinePlaybackOperations;
    private final NetworkConnectionHelper networkConnectionHelper;

    private Playa currentPlaya;
    private PlayaListener playaListener;

    // store start info so we can fallback and retry after Skippy failures
    private PropertySet lastTrackPlayed;
    private Playa.StateTransition lastStateTransition = Playa.StateTransition.DEFAULT;

    @Inject
    public StreamPlaya(Context context, MediaPlayerAdapter mediaPlayerAdapter,
                       SkippyAdapter skippyAdapter, BufferingPlaya bufferingPlaya, OfflinePlaybackOperations offlinePlaybackOperations,
                       NetworkConnectionHelper networkConnectionHelper) {

        mediaPlayaDelegate = mediaPlayerAdapter;
        skippyPlayaDelegate = skippyAdapter;
        bufferingPlayaDelegate = bufferingPlaya;
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
    public Playa.StateTransition getLastStateTransition() {
        return lastStateTransition;
    }

    public Playa.PlayaState getState() {
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
        prepareForPlay(track);

        if (isAvailableOffline(track)) {
            currentPlaya.playOffline(track, 0);
        } else {
            currentPlaya.play(track);
        }
    }

    public void play(PropertySet track, long fromPos) {
        prepareForPlay(track);

        if (isAvailableOffline(track)) {
            currentPlaya.playOffline(track, fromPos);
        } else {
            currentPlaya.play(track, fromPos);
        }
    }

    public void playUninterrupted(PropertySet track) {
        prepareForPlay(track);

        currentPlaya.playUninterrupted(track);
    }

    private void prepareForPlay(PropertySet track) {
        lastTrackPlayed = track;
        configureNextPlayaToUse();
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
    public void onPlaystateChanged(Playa.StateTransition stateTransition) {
        if (shouldFallbackToMediaPlayer(stateTransition)) {
            final long progress = skippyPlayaDelegate.getProgress();
            configureNextPlayaToUse(mediaPlayaDelegate);
            mediaPlayaDelegate.play(lastTrackPlayed, progress);
        } else {
            checkNotNull(playaListener, "Stream Player Listener is unexpectedly null when passing state");
            lastStateTransition = stateTransition;
            playaListener.onPlaystateChanged(stateTransition);
        }
    }

    private boolean shouldFallbackToMediaPlayer(Playa.StateTransition stateTransition) {
        return isUsingSkippyPlaya() && stateTransition.wasGeneralFailure() && networkConnectionHelper.isNetworkConnected();
    }

    @Override
    public void onProgressEvent(long progress, long duration) {
        playaListener.onProgressEvent(progress, duration);
    }

    @Override
    public boolean requestAudioFocus() {
        checkNotNull(playaListener, "Stream Player Listener is unexpectedly null when requesting audio focus");
        return playaListener.requestAudioFocus();
    }

    public void startBufferingMode(Urn trackUrn) {
        Playa lastPlaya = currentPlaya;
        currentPlaya = bufferingPlayaDelegate;

        lastStateTransition = new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE, trackUrn);
        onPlaystateChanged(lastStateTransition);

        if (lastPlaya != null) {
            lastPlaya.setListener(null);
            lastPlaya.stopForTrackTransition();
        }
    }

    private void configureNextPlayaToUse() {
        configureNextPlayaToUse(getNextPlaya());
    }

    private void configureNextPlayaToUse(Playa nextPlaya) {
        Log.i(TAG, "Configuring next player to use : " + nextPlaya);

        if (currentPlaya != null && currentPlaya != nextPlaya) {
            currentPlaya.stopForTrackTransition();
        }

        currentPlaya = nextPlaya;
        currentPlaya.setListener(this);
    }

    private Playa getNextPlaya() {
        if (skippyFailedToInitialize || PlaybackConstants.FORCE_MEDIA_PLAYER) {
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

}
