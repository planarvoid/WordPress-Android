package com.soundcloud.android.playback;

import android.os.Parcelable;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;

import auto.parcel.AutoParcel;

@AutoParcel
public abstract class AudioPlaybackItem implements PlaybackItem, Parcelable {

    private static AudioPlaybackItem create(PropertySet track, long startPosition, PlaybackType playbackType) {
        return new AutoParcel_AudioPlaybackItem(track.get(TrackProperty.URN), startPosition, track.get(TrackProperty.PLAY_DURATION), playbackType);
    }

    public static AudioPlaybackItem create(PropertySet track, long startPosition) {
        return create(track, startPosition, PlaybackType.AUDIO_DEFAULT);
    }

    public static AudioPlaybackItem forOffline(PropertySet track, long startPosition) {
        return create(track, startPosition, PlaybackType.AUDIO_OFFLINE);
    }

    public static AudioPlaybackItem forAudioAd(PropertySet track) {
        return create(track, 0, PlaybackType.AUDIO_UNINTERRUPTED);
    }

    @Override
    public abstract Urn getTrackUrn();

    @Override
    public abstract long getStartPosition();

    @Override
    public abstract long getDuration();

    @Override
    public abstract PlaybackType getPlaybackType();
}
