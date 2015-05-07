package com.soundcloud.android.playback;

public class PlaybackResult {

    private final boolean isSuccess;
    private final ErrorReason errorReason;

    public static enum ErrorReason {
        NONE,
        UNSKIPPABLE,
        TRACK_UNAVAILABLE_OFFLINE,
        TRACK_UNAVAILABLE_CAST,
        MISSING_PLAYABLE_TRACKS
    }

    public static PlaybackResult success() {
        return new PlaybackResult(true, ErrorReason.NONE);
    }

    public static PlaybackResult error(ErrorReason reason) {
        return new PlaybackResult(false, reason);
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public ErrorReason getErrorReason() {
        return errorReason;
    }

    private PlaybackResult(boolean isSuccess, ErrorReason errorReason) {
        this.isSuccess = isSuccess;
        this.errorReason = errorReason;
    }

}