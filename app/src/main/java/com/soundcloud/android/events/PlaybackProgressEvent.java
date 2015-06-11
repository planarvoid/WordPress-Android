package com.soundcloud.android.events;

import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.model.Urn;

public class PlaybackProgressEvent {
    PlaybackProgress playbackProgress;
    Urn trackUrn;

    public PlaybackProgressEvent(PlaybackProgress playbackProgress, Urn trackUrn) {
        this.playbackProgress = playbackProgress;
        this.trackUrn = trackUrn;
    }

    public PlaybackProgress getPlaybackProgress() {
        return playbackProgress;
    }

    public Urn getTrackUrn() {
        return trackUrn;
    }
}
