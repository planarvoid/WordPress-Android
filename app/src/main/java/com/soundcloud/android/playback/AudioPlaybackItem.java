package com.soundcloud.android.playback;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.Track;

import android.os.Parcelable;

@AutoValue
public abstract class AudioPlaybackItem implements PlaybackItem, Parcelable {

    private static AudioPlaybackItem create(Track track, long startPosition, PlaybackType playbackType) {
        return new AutoValue_AudioPlaybackItem(track.urn(),
                                                startPosition,
                                                Durations.getTrackPlayDuration(track),
                                                playbackType);
    }

    public static AudioPlaybackItem create(Urn trackUrn, long startPosition, long duration, PlaybackType playbackType) {
        return new AutoValue_AudioPlaybackItem(trackUrn, startPosition, duration, playbackType);
    }

    public static AudioPlaybackItem create(Track track, long startPosition) {
        return create(track, startPosition, PlaybackType.AUDIO_DEFAULT);
    }

    public static AudioPlaybackItem forSnippet(Track track, long startPosition) {
        return create(track, startPosition, PlaybackType.AUDIO_SNIPPET);
    }

    public static AudioPlaybackItem forOffline(Track track, long startPosition) {
        return create(track, startPosition, PlaybackType.AUDIO_OFFLINE);
    }

    @Override
    public abstract Urn getUrn();

    @Override
    public abstract long getStartPosition();

    @Override
    public abstract long getDuration();

    @Override
    public abstract PlaybackType getPlaybackType();

}
