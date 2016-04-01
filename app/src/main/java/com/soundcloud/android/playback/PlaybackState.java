package com.soundcloud.android.playback;

/**
 * PLAYING : the internal player is currently playing
 * IDLE : this internal player is not playing, nor is there intent to play
 * BUFFERING : there is intent to play, but sound is not coming out of the speakers
 * Note : there is no state for buffering with no intent to play. We should just report that as IDLE
 */
public enum PlaybackState {
    BUFFERING, PLAYING, IDLE;

    /**
     * User Intent. e.g., should we show the play button or pause button *
     */
    public boolean isPlaying() {
        return this == PLAYING || this == BUFFERING;
    }

    public boolean isIdle() {
        return this == IDLE;
    }

    /**
     * Actual playback state. Is there sound coming out of the speakers or not *
     */
    public boolean isPlayerPlaying() {
        return this == PLAYING;
    }

    public boolean isBuffering() {
        return this == BUFFERING;
    }

}
