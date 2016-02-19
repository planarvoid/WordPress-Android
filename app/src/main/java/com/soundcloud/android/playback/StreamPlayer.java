package com.soundcloud.android.playback;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.Player.PlayerListener;
import com.soundcloud.android.playback.mediaplayer.MediaPlayerAdapter;
import com.soundcloud.android.playback.skippy.SkippyAdapter;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;

class StreamPlayer implements PlayerListener {

    public static final String TAG = "StreamPlayer";

    @VisibleForTesting
    static boolean skippyFailedToInitialize;

    private final MediaPlayerAdapter mediaPlayerDelegate;
    private final SkippyAdapter skippyPlayerDelegate;
    private final NetworkConnectionHelper networkConnectionHelper;
    private final EventBus eventBus;

    private Player currentPlayer;
    private PlayerListener playerListener;

    // store start info so we can fallback and retry after Skippy failures
    private PlaybackItem lastItemPlayed;
    private Player.StateTransition lastStateTransition = Player.StateTransition.DEFAULT;

    @Inject
    public StreamPlayer(MediaPlayerAdapter mediaPlayerAdapter,
                        SkippyAdapter skippyAdapter,
                        NetworkConnectionHelper networkConnectionHelper,
                        EventBus eventBus) {
        mediaPlayerDelegate = mediaPlayerAdapter;
        skippyPlayerDelegate = skippyAdapter;
        this.networkConnectionHelper = networkConnectionHelper;
        this.eventBus = eventBus;

        if (!skippyFailedToInitialize) {
            skippyFailedToInitialize = !skippyPlayerDelegate.init();
        }

        currentPlayer = skippyFailedToInitialize ? mediaPlayerAdapter : skippyAdapter;
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

    public void play(PlaybackItem playbackItem) {
        prepareForPlay(playbackItem);
        currentPlayer.play(playbackItem);
    }

    public void preload(PreloadItem preloadItem) {
        skippyPlayerDelegate.preload(preloadItem);
    }

    private void prepareForPlay(PlaybackItem playbackItem) {
        lastItemPlayed = playbackItem;
        configureNextPlayerToUse(playbackItem);
    }

    public void resume() {
        currentPlayer.resume();
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
            final long currentProgress = skippyPlayerDelegate.getProgress();
            final PlaybackItem updatedItem = AudioPlaybackItem.create(lastItemPlayed.getUrn(), currentProgress, lastItemPlayed.getDuration(), lastItemPlayed.getPlaybackType());
            configureNextPlayerToUse(mediaPlayerDelegate);
            mediaPlayerDelegate.play(updatedItem);
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

    private void configureNextPlayerToUse(PlaybackItem playbackItem) {
        configureNextPlayerToUse(getNextPlayer(playbackItem));
    }

    private void configureNextPlayerToUse(Player nextPlayer) {
        Log.i(TAG, "Configuring next player to use : " + nextPlayer);

        if (currentPlayer != null && currentPlayer != nextPlayer) {
            currentPlayer.stopForTrackTransition();
        }

        currentPlayer = nextPlayer;
        currentPlayer.setListener(this);
    }

    private Player getNextPlayer(PlaybackItem playbackItem) {
        if (playbackItem.getPlaybackType() == PlaybackType.AUDIO_OFFLINE && skippyFailedToInitialize) {
            logOfflinePlayNotAvailable();
        }
        if (playbackItem.getPlaybackType() == PlaybackType.VIDEO_DEFAULT || skippyFailedToInitialize) {
            return mediaPlayerDelegate;
        } else {
            return skippyPlayerDelegate;
        }
    }

    private boolean isUsingSkippyPlayer() {
        return currentPlayer == skippyPlayerDelegate;
    }

    private void logOfflinePlayNotAvailable() {
        ErrorUtils.handleSilentException(new StreamPlayer.OfflinePlayUnavailableException());
        eventBus.publish(EventQueue.PLAYBACK_ERROR, getOfflinePlayUnavailableErrorEvent());
    }

    @NonNull
    private PlaybackErrorEvent getOfflinePlayUnavailableErrorEvent() {
        return new PlaybackErrorEvent(PlaybackErrorEvent.CATEGORY_OFFLINE_PLAY_UNAVAILABLE, PlaybackProtocol.HLS,
                    Strings.EMPTY, ConnectionType.UNKNOWN);
    }

    private static class OfflinePlayUnavailableException extends Exception {
    }
}
