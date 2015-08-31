package com.soundcloud.android.playback;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.playback.Player.PlayerListener;
import com.soundcloud.android.playback.mediaplayer.MediaPlayerAdapter;
import com.soundcloud.android.playback.skippy.SkippyAdapter;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.PropertySet;

import android.content.Context;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;

class StreamPlayer implements PlayerListener {

    public static final String TAG = "StreamPlayer";

    @VisibleForTesting
    static boolean skippyFailedToInitialize;

    private final MediaPlayerAdapter mediaPlayerDelegate;
    private final SkippyAdapter skippyPlayerDelegate;
    private final BufferingPlayer bufferingPlayerDelegate;
    private final OfflinePlaybackOperations offlinePlaybackOperations;
    private final NetworkConnectionHelper networkConnectionHelper;

    private Player currentPlayer;
    private PlayerListener playerListener;

    // store start info so we can fallback and retry after Skippy failures
    private PropertySet lastTrackPlayed;
    private Player.StateTransition lastStateTransition = Player.StateTransition.DEFAULT;

    @Inject
    public StreamPlayer(Context context, MediaPlayerAdapter mediaPlayerAdapter,
                        SkippyAdapter skippyAdapter, BufferingPlayer bufferingPlayer, OfflinePlaybackOperations offlinePlaybackOperations,
                        NetworkConnectionHelper networkConnectionHelper) {

        mediaPlayerDelegate = mediaPlayerAdapter;
        skippyPlayerDelegate = skippyAdapter;
        bufferingPlayerDelegate = bufferingPlayer;
        this.offlinePlaybackOperations = offlinePlaybackOperations;
        this.networkConnectionHelper = networkConnectionHelper;
        currentPlayer = bufferingPlayerDelegate;

        if (!skippyFailedToInitialize) {
            skippyFailedToInitialize = !skippyPlayerDelegate.init(context);
        }
    }

    /**
     * state storage. should be gone in player v2 *
     */

    @Deprecated
    public Player.StateTransition getLastStateTransition() {
        return lastStateTransition;
    }

    public Player.PlayerState getState() {
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
            currentPlayer.playOffline(track, 0);
        } else {
            currentPlayer.play(track);
        }
    }

    public void play(PropertySet track, long fromPos) {
        prepareForPlay(track);

        if (isAvailableOffline(track)) {
            currentPlayer.playOffline(track, fromPos);
        } else {
            currentPlayer.play(track, fromPos);
        }
    }

    public void playUninterrupted(PropertySet track) {
        prepareForPlay(track);

        currentPlayer.playUninterrupted(track);
    }

    private void prepareForPlay(PropertySet track) {
        lastTrackPlayed = track;
        configureNextPlayerToUse();
    }

    public boolean resume() {
        return currentPlayer.resume();
    }

    public void pause() {
        currentPlayer.pause();
    }

    public long seek(long ms, boolean performSeek) {
        return currentPlayer.seek(ms, performSeek);
    }

    public long getProgress() {
        return currentPlayer.getProgress();
    }

    public void setVolume(float v) {
        currentPlayer.setVolume(v);
    }

    public void stop() {
        currentPlayer.stop();
    }

    public boolean isSeekable() {
        return currentPlayer.isSeekable();
    }

    public void destroy() {
        // call stop first as it will save the queue/position
        mediaPlayerDelegate.destroy();
        if (!skippyFailedToInitialize) {
            skippyPlayerDelegate.destroy();
        }
    }

    public void setListener(PlayerListener playerListener) {
        this.playerListener = playerListener;
        if (currentPlayer != null) {
            currentPlayer.setListener(playerListener);
        }
    }

    @Override
    public void onPlaystateChanged(Player.StateTransition stateTransition) {
        if (shouldFallbackToMediaPlayer(stateTransition)) {
            final long progress = skippyPlayerDelegate.getProgress();
            configureNextPlayerToUse(mediaPlayerDelegate);
            mediaPlayerDelegate.play(lastTrackPlayed, progress);
        } else {
            checkNotNull(playerListener, "Stream Player Listener is unexpectedly null when passing state");
            lastStateTransition = stateTransition;
            playerListener.onPlaystateChanged(stateTransition);
        }
    }

    private boolean shouldFallbackToMediaPlayer(Player.StateTransition stateTransition) {
        return isUsingSkippyPlayer() && stateTransition.wasGeneralFailure() && networkConnectionHelper.isNetworkConnected();
    }

    @Override
    public void onProgressEvent(long progress, long duration) {
        playerListener.onProgressEvent(progress, duration);
    }

    @Override
    public boolean requestAudioFocus() {
        checkNotNull(playerListener, "Stream Player Listener is unexpectedly null when requesting audio focus");
        return playerListener.requestAudioFocus();
    }

    public void startBufferingMode(Urn trackUrn) {
        Player lastPlayer = currentPlayer;
        currentPlayer = bufferingPlayerDelegate;

        lastStateTransition = new Player.StateTransition(Player.PlayerState.BUFFERING, Player.Reason.NONE, trackUrn);
        onPlaystateChanged(lastStateTransition);

        if (lastPlayer != null) {
            lastPlayer.setListener(null);
            lastPlayer.stopForTrackTransition();
        }
    }

    private void configureNextPlayerToUse() {
        configureNextPlayerToUse(getNextPlayer());
    }

    private void configureNextPlayerToUse(Player nextPlayer) {
        Log.i(TAG, "Configuring next player to use : " + nextPlayer);

        if (currentPlayer != null && currentPlayer != nextPlayer) {
            currentPlayer.stopForTrackTransition();
        }

        currentPlayer = nextPlayer;
        currentPlayer.setListener(this);
    }

    private Player getNextPlayer() {
        if (skippyFailedToInitialize || PlaybackConstants.FORCE_MEDIA_PLAYER) {
            return mediaPlayerDelegate;
        }
        return skippyPlayerDelegate;
    }

    private boolean isAvailableOffline(PropertySet track) {
        return !skippyFailedToInitialize && offlinePlaybackOperations.shouldPlayOffline(track);
    }

    private boolean isUsingSkippyPlayer() {
        return currentPlayer == skippyPlayerDelegate;
    }

}
