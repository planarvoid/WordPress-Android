package com.soundcloud.android.playback;

public interface PlaybackStrategy {

    void togglePlayback();

    void resume();

    void pause();

    void playCurrent();

    void playCurrent(long fromPosition);

    void seek(long position);
}