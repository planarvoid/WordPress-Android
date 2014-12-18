package com.soundcloud.android.playback;

import com.soundcloud.android.cast.CastPlayer;

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
    public void seek(long position) {
        castPlayer.seek(position);
    }
}
