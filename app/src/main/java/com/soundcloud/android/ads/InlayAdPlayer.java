package com.soundcloud.android.ads;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.VideoAdPlaybackItem;
import com.soundcloud.android.playback.mediaplayer.MediaPlayerAdapter;
import com.soundcloud.android.utils.Log;

import javax.inject.Inject;

class InlayAdPlayer implements Player.PlayerListener {

    private Player currentPlayer;
    private PlaybackStateTransition lastState = PlaybackStateTransition.DEFAULT;

    @Inject
    public InlayAdPlayer(MediaPlayerAdapter mediaPlayerAdapter) {
        currentPlayer = mediaPlayerAdapter;
        currentPlayer.setListener(this);
    }

    void play(VideoAdPlaybackItem playbackItem) {
        final Urn urn = playbackItem.getUrn();

        if (!alreadyInitiatedPlaybackForUrn(urn)) {
            if (wasPaused(urn)) {
                currentPlayer.resume(playbackItem);
            } else {
                currentPlayer.stopForTrackTransition();
                currentPlayer.play(playbackItem);
            }
        }
    }

    void pause() {
        currentPlayer.pause();
    }

    boolean isPlaying() {
        return lastState.isPlayerPlaying();
    }

    private boolean alreadyInitiatedPlaybackForUrn(Urn urn) {
        return lastState.isForUrn(urn) && (lastState.isPlayerPlaying() || lastState.isBuffering());
    }

    private boolean wasPaused(Urn urn) {
        return lastState.isForUrn(urn) && lastState.isPaused();
    }

    @Override
    public void onPlaystateChanged(PlaybackStateTransition stateTransition) {
        lastState = stateTransition;
        // TODO: Feed these events into AdSessionAnalyticsDispatcher for event logger playback events \o/
        Log.d(Log.ADS_TAG, "Video inlay state change:" + stateTransition.toString());
    }

    @Override
    public void onProgressEvent(long progress, long duration) {
        // TODO: Feed these events into AdSessionAnalyticsDispatcher for event logger checkpoint + quartile events \o/
        Log.d(Log.ADS_TAG, "Video inlay progress update:" + progress + "/" + duration);
    }
}
