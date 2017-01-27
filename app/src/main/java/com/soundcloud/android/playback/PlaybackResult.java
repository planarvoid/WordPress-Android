package com.soundcloud.android.playback;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class PlaybackResult {

    public enum ErrorReason {
        NONE,
        UNSKIPPABLE,
        TRACK_UNAVAILABLE_OFFLINE,
        TRACK_UNAVAILABLE_CAST,
        MISSING_PLAYABLE_TRACKS
    }

    public abstract boolean isSuccess();

    public abstract ErrorReason getErrorReason();

    public static PlaybackResult success() {
        return new AutoValue_PlaybackResult(true, ErrorReason.NONE);
    }

    public static PlaybackResult error(ErrorReason reason) {
        return new AutoValue_PlaybackResult(false, reason);
    }

}