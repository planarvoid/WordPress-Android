package com.soundcloud.android.events;

import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.playback.PlaybackProgress;

public class PlaybackProgressEvent {
    PlaybackProgress playbackProgress;
    TrackUrn trackUrn;

    public PlaybackProgressEvent(PlaybackProgress playbackProgress, TrackUrn trackUrn) {
        this.playbackProgress = playbackProgress;
        this.trackUrn = trackUrn;
    }

    public PlaybackProgress getPlaybackProgress() {
        return playbackProgress;
    }

    public TrackUrn getTrackUrn() {
        return trackUrn;
    }
}
