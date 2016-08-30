package com.soundcloud.android.playback;

import android.os.Parcelable;

import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.AudioAdSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import java.util.List;

import auto.parcel.AutoParcel;

@AutoParcel
public abstract class AudioAdPlaybackItem implements AdPlaybackItem, Parcelable {

    private static final long POSITION_START = 0L;

    public static AudioAdPlaybackItem create(PropertySet track, AudioAd audioAd) {
        return new AutoParcel_AudioAdPlaybackItem(
                audioAd,
                track.get(TrackProperty.URN),
                audioAd.getAudioSources(),
                POSITION_START,
                PlaybackType.AUDIO_AD,
                Durations.getTrackPlayDuration(track));
    }

    @Override
    public abstract Urn getUrn();

    public abstract List<AudioAdSource> getSources();

    @Override
    public abstract long getStartPosition();

    @Override
    public abstract PlaybackType getPlaybackType();

    @Override
    public abstract long getDuration();

}
