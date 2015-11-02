package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class PlaybackProgressEvent {
    public static PlaybackProgressEvent forTrack(PlaybackProgress playbackProgress, Urn trackUrn) {
        return new AutoValue_PlaybackProgressEvent(playbackProgress, Optional.of(trackUrn), Optional.<String>absent());
    }

    public static PlaybackProgressEvent forVideo(PlaybackProgress playbackProgress, String videoAdUrn) {
        return new AutoValue_PlaybackProgressEvent(playbackProgress, Optional.<Urn>absent(),  Optional.of(videoAdUrn));
    }

    public abstract PlaybackProgress getPlaybackProgress();

    public abstract Optional<Urn> getTrackUrn();

    public abstract Optional<String> getVideoAdUrn();

    public boolean isForTrack() {
        return getTrackUrn().isPresent();
    }

    public boolean isForVideo() {
        return getVideoAdUrn().isPresent();
    }
}
