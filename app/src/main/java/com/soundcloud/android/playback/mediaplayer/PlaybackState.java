package com.soundcloud.android.playback.mediaplayer;

import java.util.EnumSet;
/**
 * States the mediaplayer can be in - we need to track these manually.
 */
    enum PlaybackState {
    STOPPED,            // initial state, or stopped
    ERROR,              // onError() was called
    ERROR_RETRYING,     // onError() + retry
    PREPARING,          // initial buffering
    PLAYING,            // currently playing
    PAUSED,             // paused by user
    PAUSED_FOR_BUFFERING, // paused by framework
    COMPLETED;            // onComplete() was called

    // see Valid and invalid states on http://developer.android.com/reference/android/media/MediaPlayer.html
    public static final EnumSet<PlaybackState> SEEKABLE =
            EnumSet.of(PREPARING, PLAYING, PAUSED, PAUSED_FOR_BUFFERING, COMPLETED);

    public static final EnumSet<PlaybackState> STARTABLE =
            EnumSet.of(PLAYING, PAUSED, PAUSED_FOR_BUFFERING, COMPLETED);

    public static final EnumSet<PlaybackState> STOPPABLE =
            EnumSet.of(PLAYING, STOPPED, PAUSED, PAUSED_FOR_BUFFERING, COMPLETED);

    public static final EnumSet<PlaybackState> PAUSEABLE =
            EnumSet.of(PLAYING, PAUSED_FOR_BUFFERING, PAUSED);

    public static final EnumSet<PlaybackState> LOADING =
            EnumSet.of(PAUSED_FOR_BUFFERING, PREPARING);

    public boolean isPausable() {
        return PAUSEABLE.contains(this);
    }

    public boolean isStartable() {
        return STARTABLE.contains(this);
    }

    public boolean isSeekable() {
        return SEEKABLE.contains(this);
    }

    public boolean isStoppable() {
        return STOPPABLE.contains(this);
    }

    public boolean isLoading() {
        return LOADING.contains(this);
    }

    public boolean isError() {
        return this == ERROR || this == ERROR_RETRYING;
    }

    public boolean canGetMPProgress() {
        return !isError() && this != PREPARING && this != STOPPED;
    }

    // is the service currently playing, or about to play soon?
    public boolean isSupposedToBePlaying() {
        return this == PREPARING || this == PLAYING || this == PAUSED_FOR_BUFFERING;
    }
}
