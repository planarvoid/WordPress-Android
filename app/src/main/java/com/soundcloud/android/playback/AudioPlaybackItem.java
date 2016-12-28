package com.soundcloud.android.playback;

import auto.parcel.AutoParcel;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackItem;

import android.os.Parcelable;

@AutoParcel
public abstract class AudioPlaybackItem implements PlaybackItem, Parcelable {

    private static AudioPlaybackItem create(TrackItem track, long startPosition, PlaybackType playbackType) {
        return new AutoParcel_AudioPlaybackItem(track.getUrn(),
                                                startPosition,
                                                Durations.getTrackPlayDuration(track),
                                                playbackType);
    }

    public static AudioPlaybackItem create(Urn trackUrn, long startPosition, long duration, PlaybackType playbackType) {
        return new AutoParcel_AudioPlaybackItem(trackUrn, startPosition, duration, playbackType);
    }

    public static AudioPlaybackItem create(TrackItem track, long startPosition) {
        return create(track, startPosition, PlaybackType.AUDIO_DEFAULT);
    }

    public static AudioPlaybackItem forSnippet(TrackItem track, long startPosition) {
        return create(track, startPosition, PlaybackType.AUDIO_SNIPPET);
    }

    public static AudioPlaybackItem forOffline(TrackItem track, long startPosition) {
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
