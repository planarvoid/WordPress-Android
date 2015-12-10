package com.soundcloud.android.playback;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.Player.PlayerListener;
import com.soundcloud.android.playback.mediaplayer.MediaPlayerAdapter;
import com.soundcloud.android.playback.mediaplayer.VideoPlayerAdapter;
import com.soundcloud.android.playback.skippy.SkippyAdapter;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.NetworkConnectionHelper;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;

class StreamPlayer implements PlayerListener {

    public static final String TAG = "StreamPlayer";

    @VisibleForTesting
    static boolean skippyFailedToInitialize;

    private final MediaPlayerAdapter mediaPlayerDelegate;
    private final VideoPlayerAdapter videoPlayerDelegate;
    private final SkippyAdapter skippyPlayerDelegate;
    private final NetworkConnectionHelper networkConnectionHelper;

    private Player currentPlayer;
    private PlayerListener playerListener;

    // store start info so we can fallback and retry after Skippy failures
    private PlaybackItem lastTrackPlayed;
    private Player.StateTransition lastStateTransition = Player.StateTransition.DEFAULT;

    @Inject
    public StreamPlayer(MediaPlayerAdapter mediaPlayerAdapter,
                        VideoPlayerAdapter videoPlayerAdapter,
                        SkippyAdapter skippyAdapter,
                        NetworkConnectionHelper networkConnectionHelper) {
        mediaPlayerDelegate = mediaPlayerAdapter;
        videoPlayerDelegate = videoPlayerAdapter;
        skippyPlayerDelegate = skippyAdapter;
        this.networkConnectionHelper = networkConnectionHelper;

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

        switch(playbackItem.getPlaybackType()) {
            case AUDIO_DEFAULT:
                currentPlayer.play(playbackItem.getTrackUrn(), playbackItem.getStartPosition());
                break;
            case AUDIO_OFFLINE:
                currentPlayer.playOffline(playbackItem.getTrackUrn(), playbackItem.getStartPosition());
                break;
            case AUDIO_UNINTERRUPTED:
                currentPlayer.playUninterrupted(playbackItem.getTrackUrn());
                break;
            case VIDEO_DEFAULT:
                currentPlayer.playVideo((VideoPlaybackItem) playbackItem);
        }
    }

    public void preload(Urn urn) {
        skippyPlayerDelegate.preload(urn);
    }

    private void prepareForPlay(PlaybackItem playbackItem) {
        lastTrackPlayed = playbackItem;
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
        videoPlayerDelegate.destroy();
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
            mediaPlayerDelegate.play(lastTrackPlayed.getTrackUrn(), progress);
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
        if (playbackItem.getPlaybackType() == PlaybackType.VIDEO_DEFAULT) {
            return videoPlayerDelegate;
        } else if (skippyFailedToInitialize || PlaybackConstants.FORCE_MEDIA_PLAYER) {
            return mediaPlayerDelegate;
        }
        return skippyPlayerDelegate;
    }

    private boolean isUsingSkippyPlayer() {
        return currentPlayer == skippyPlayerDelegate;
    }
}
