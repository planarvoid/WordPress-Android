package com.soundcloud.android.playback;

import com.soundcloud.android.cast.CastPlayer;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlaySessionSource;

import java.util.List;

public class CastPlaybackStrategy implements PlaybackStrategy {

    private final CastPlayer castPlayer;

    public CastPlaybackStrategy(CastPlayer castPlayer) {
        this.castPlayer = castPlayer;
    }

    @Override
    public void togglePlayback() {
        castPlayer.togglePlayback();
    }

    @Override
    public void resume() {
        castPlayer.resume();
    }

    @Override
    public void pause() {
        castPlayer.pause();
    }

    @Override
    public void playCurrent() {
        castPlayer.playCurrent();
    }

    @Override
    public void playCurrent(long fromPosition) {
        castPlayer.playCurrent(fromPosition);
    }

    @Override
    public void playNewQueue(List<Urn> playQueueTracks, Urn initialTrackUrn, int initialTrackPosition, PlaySessionSource playSessionSource) {
        castPlayer.playNewQueue(playQueueTracks, initialTrackUrn, initialTrackPosition, playSessionSource);
    }

    @Override
    public void seek(long position) {
        castPlayer.seek(position);
    }
}
