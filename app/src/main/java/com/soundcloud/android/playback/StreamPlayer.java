package com.soundcloud.android.playback;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.playback.Player.PlayerListener;
import com.soundcloud.android.playback.mediaplayer.MediaPlayerAdapter;
import com.soundcloud.android.playback.skippy.SkippyAdapter;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;

class StreamPlayer implements PlayerListener {

    public static final String TAG = "StreamPlayer";

    @VisibleForTesting static boolean SKIPPY_FAILED_TO_INITIALIZE;

    private final MediaPlayerAdapter mediaPlayerDelegate;
    private final Optional<SkippyAdapter> skippyPlayerDelegate;

    private final Optional<? extends Player> offlineContentPlayer;
    private final Player videoPlayer;
    private final Player defaultPlayer;

    private final NetworkConnectionHelper networkConnectionHelper;
    private final EventBus eventBus;

    private Player currentPlayer;
    private PlayerListener playerListener;

    // store start info so we can fallback and retry after Skippy failures
    private PlaybackItem lastItemPlayed;
    private PlaybackStateTransition lastStateTransition = PlaybackStateTransition.DEFAULT;

    @Inject
    StreamPlayer(MediaPlayerAdapter mediaPlayerAdapter,
                 SkippyAdapter skippyAdapter,
                 NetworkConnectionHelper networkConnectionHelper,
                 EventBus eventBus) {

        this.networkConnectionHelper = networkConnectionHelper;
        this.eventBus = eventBus;

        this.mediaPlayerDelegate = mediaPlayerAdapter;
        this.skippyPlayerDelegate = initSkippy(skippyAdapter);

        this.defaultPlayer = defaultPlayer();
        this.offlineContentPlayer = skippyPlayerDelegate;
        this.videoPlayer = mediaPlayerAdapter;
        this.currentPlayer = defaultPlayer;
    }

    private static Optional<SkippyAdapter> initSkippy(SkippyAdapter skippyAdapter) {
        if (SKIPPY_FAILED_TO_INITIALIZE) {
            return Optional.absent();
        } else {
            SKIPPY_FAILED_TO_INITIALIZE = !skippyAdapter.init();
            if (SKIPPY_FAILED_TO_INITIALIZE) {
                return Optional.absent();
            } else {
                return Optional.of(skippyAdapter);
            }
        }
    }

    private Player defaultPlayer() {
        if (skippyPlayerDelegate.isPresent()) {
            return skippyPlayerDelegate.get();
        } else {
            return mediaPlayerDelegate;
        }
    }

    /**
     * state storage. should be gone in player v2 *
     */
    @Deprecated
    PlaybackStateTransition getLastStateTransition() {
        return lastStateTransition;
    }

    public PlaybackState getState() {
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
        currentPlayer.preload(preloadItem);
    }

    private void prepareForPlay(PlaybackItem playbackItem) {
        lastItemPlayed = playbackItem;
        configureNextPlayerToUse(playbackItem);
    }

    public void resume(PlaybackItem playbackItem) {
        currentPlayer.resume(playbackItem);
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

    void setVolume(float v) {
        currentPlayer.setVolume(v);
    }

    float getVolume() {
        return currentPlayer.getVolume();
    }

    public void stop() {
        currentPlayer.stop();
    }

    boolean isSeekable() {
        return currentPlayer.isSeekable();
    }

    public void destroy() {
        // call stop first as it will save the queue/position
        mediaPlayerDelegate.destroy();
        if (skippyPlayerDelegate.isPresent()) {
            skippyPlayerDelegate.get().destroy();
        }
    }

    public void setListener(PlayerListener playerListener) {
        this.playerListener = playerListener;
        currentPlayer.setListener(playerListener);
    }

    @Override
    public void onPlaystateChanged(PlaybackStateTransition stateTransition) {
        if (shouldFallbackToMediaPlayer(stateTransition)) {
            final long currentProgress = currentPlayer.getProgress();
            configureNextPlayerToUse(mediaPlayerDelegate);
            currentPlayer.play(getUpdatedItem(currentProgress));
        } else {
            checkNotNull(playerListener, "Stream Player Listener is unexpectedly null when passing state");
            lastStateTransition = stateTransition;
            playerListener.onPlaystateChanged(stateTransition);
        }
    }

    private PlaybackItem getUpdatedItem(long currentProgress) {
        switch (lastItemPlayed.getPlaybackType()) {
            case AUDIO_AD:
            case VIDEO_AD:
                return lastItemPlayed;
            default:
                return AudioPlaybackItem.create(lastItemPlayed.getUrn(),
                                                currentProgress,
                                                lastItemPlayed.getDuration(),
                                                lastItemPlayed.getPlaybackType());
        }
    }

    private boolean shouldFallbackToMediaPlayer(PlaybackStateTransition stateTransition) {
        return isNotUsingMediaPlayer()
                && stateTransition.wasGeneralFailure() && networkConnectionHelper.isNetworkConnected()
                && lastItemPlayed.getPlaybackType() != PlaybackType.AUDIO_OFFLINE;
    }

    @Override
    public void onProgressEvent(long progress, long duration) {
        playerListener.onProgressEvent(progress, duration);
    }

    private void configureNextPlayerToUse(PlaybackItem playbackItem) {
        configureNextPlayerToUse(getNextPlayer(playbackItem));
    }

    private void configureNextPlayerToUse(Player nextPlayer) {
        Log.i(TAG, "Configuring next player to use : " + nextPlayer);

        if (currentPlayer != nextPlayer) {
            currentPlayer.stopForTrackTransition();
        }

        currentPlayer = nextPlayer;
        currentPlayer.setListener(this);
    }

    private Player getNextPlayer(PlaybackItem playbackItem) {
        if (playbackItem.getPlaybackType() == PlaybackType.AUDIO_OFFLINE && !offlineContentPlayer.isPresent()) {
            logOfflinePlayNotAvailable();
        }

        switch (playbackItem.getPlaybackType()) {
            case VIDEO_AD:
                return videoPlayer;
            case AUDIO_OFFLINE:
                return offlineContentPlayer.isPresent() ? offlineContentPlayer.get() : defaultPlayer;
            case AUDIO_DEFAULT:
            case AUDIO_SNIPPET:
            case AUDIO_AD:
                return defaultPlayer;
            default:
                throw new IllegalArgumentException("Unknown playback type: " + playbackItem.getPlaybackType());
        }
    }

    private boolean isNotUsingMediaPlayer() {
        return currentPlayer != mediaPlayerDelegate;
    }

    private void logOfflinePlayNotAvailable() {
        ErrorUtils.handleSilentException(new StreamPlayer.OfflinePlayUnavailableException());
        eventBus.publish(EventQueue.PLAYBACK_ERROR, getOfflinePlayUnavailableErrorEvent());
    }

    private PlaybackErrorEvent getOfflinePlayUnavailableErrorEvent() {
        return new PlaybackErrorEvent(PlaybackErrorEvent.CATEGORY_OFFLINE_PLAY_UNAVAILABLE,
                                      PlaybackProtocol.HLS,
                                      Strings.EMPTY,
                                      ConnectionType.UNKNOWN);
    }

    private static class OfflinePlayUnavailableException extends Exception {
    }
}
