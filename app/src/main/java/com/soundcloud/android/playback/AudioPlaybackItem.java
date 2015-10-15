package com.soundcloud.android.playback;

import auto.parcel.AutoParcel;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;

import android.os.Parcelable;

@AutoParcel
public abstract class AudioPlaybackItem implements PlaybackItem, Parcelable {

    public static AudioPlaybackItem create(PropertySet track, long startPosition, PlaybackType playbackType) {
        return new AutoParcel_AudioPlaybackItem(track.get(TrackProperty.URN), startPosition, track.get(TrackProperty.DURATION), playbackType);
    }

    public static AudioPlaybackItem create(PropertySet track, long startPosition) {
        return create(track, startPosition, PlaybackType.DEFAULT);
    }

    public static AudioPlaybackItem forOffline(PropertySet track, long startPosition) {
        return create(track, startPosition, PlaybackType.OFFLINE);
    }

    public static AudioPlaybackItem forAudioAd(PropertySet track) {
        return create(track, 0, PlaybackType.UNINTERRUPTED);
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
