package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;

@AutoValue
public abstract class PlaybackProgressEvent {
    public static PlaybackProgressEvent create(PlaybackProgress playbackProgress, Urn itemUrn) {
        return new AutoValue_PlaybackProgressEvent(playbackProgress, itemUrn);
    }

    public abstract PlaybackProgress getPlaybackProgress();

    public abstract Urn getUrn();
}
