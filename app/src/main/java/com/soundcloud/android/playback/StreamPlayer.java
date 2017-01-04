package com.soundcloud.android.playback;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.playback.Player.PlayerListener;
import com.soundcloud.android.playback.flipper.FlipperAdapter;
import com.soundcloud.android.playback.mediaplayer.MediaPlayerAdapter;
import com.soundcloud.android.playback.skippy.SkippyAdapter;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Provider;

class StreamPlayer implements PlayerListener {

    public static final String TAG = "StreamPlayer";

    @VisibleForTesting static boolean SKIPPY_FAILED_TO_INITIALIZE;

    private final MediaPlayerAdapter mediaPlayerDelegate;
    private final Optional<SkippyAdapter> skippyPlayerDelegate;
    private final Optional<FlipperAdapter> flipperPlayerDelegate;

    private final Optional<? extends Player> offlineContentPlayer;
    private final Player videoPlayer;
    private final Player audioAdPlayer;
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
                 Provider<FlipperAdapter> flipperAdapterProvider,
                 NetworkConnectionHelper networkConnectionHelper,
                 EventBus eventBus,
                 FeatureFlags featureFlags) {

        this.networkConnectionHelper = networkConnectionHelper;
        this.eventBus = eventBus;

        this.mediaPlayerDelegate = mediaPlayerAdapter;
        this.skippyPlayerDelegate = initSkippy(skippyAdapter);
        this.flipperPlayerDelegate = featureFlags.isEnabled(Flag.FLIPPER) ? Optional.of(flipperAdapterProvider.get()) : Optional.<FlipperAdapter>absent();

        this.defaultPlayer = defaultPlayer();
        this.offlineContentPlayer = skippyPlayerDelegate;
        this.audioAdPlayer = skippyPlayerDelegate.isPresent() ? skippyPlayerDelegate.get() : mediaPlayerAdapter;
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
        if (flipperPlayerDelegate.isPresent()) {
            return flipperPlayerDelegate.get();
        } else if (skippyPlayerDelegate.isPresent()) {
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
        lastItemPlayed = playbackItem;
        final Optional<Player> nextPlayer = getNextPlayer(playbackItem);
        if (nextPlayer.isPresent()) {
            configureNextPlayerToUse(nextPlayer.get());
            currentPlayer.play(playbackItem);
        } else {
            publishOfflinePlayNotAvailable();
        }
    }

    public void preload(PreloadItem preloadItem) {
        currentPlayer.preload(preloadItem);
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
        if (flipperPlayerDelegate.isPresent()) {
            flipperPlayerDelegate.get().destroy();
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

    private void configureNextPlayerToUse(Player nextPlayer) {
        Log.i(TAG, "Configuring next player to use : " + nextPlayer);

        if (currentPlayer != nextPlayer) {
            currentPlayer.stopForTrackTransition();
        }

        currentPlayer = nextPlayer;
        currentPlayer.setListener(this);
    }

    private Optional<Player> getNextPlayer(PlaybackItem playbackItem) {
        final Player player;
        switch (playbackItem.getPlaybackType()) {
            case AUDIO_AD:
                player = audioAdPlayer;
                break;
            case VIDEO_AD:
                player = videoPlayer;
                break;
            case AUDIO_OFFLINE:
                player = offlineContentPlayer.isPresent() ? offlineContentPlayer.get() : null;
                break;
            case AUDIO_DEFAULT:
            case AUDIO_SNIPPET:
                player = defaultPlayer;
                break;
            default:
                throw new IllegalArgumentException("Unknown playback type: " + playbackItem.getPlaybackType());
        }
        return Optional.fromNullable(player);
    }

    private boolean isNotUsingMediaPlayer() {
        return currentPlayer != mediaPlayerDelegate;
    }

    private void publishOfflinePlayNotAvailable() {
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
