package com.soundcloud.android.playback;

import android.os.Parcelable;

import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;

import auto.parcel.AutoParcel;

@AutoParcel
public abstract class AudioAdPlaybackItem implements PlaybackItem, Parcelable {

    private static final long POSITION_START = 0L;

    public static AudioAdPlaybackItem create(PropertySet track, AudioAd audioAd) {
        return new AutoParcel_AudioAdPlaybackItem(
                track.get(TrackProperty.URN),
                POSITION_START,
                PlaybackType.AUDIO_AD,
                Durations.getTrackPlayDuration(track),
                audioAd.isThirdParty(),
                audioAd.getStreamUrl()
        );
    }

    @Override
    public abstract Urn getUrn();

    @Override
    public abstract long getStartPosition();

    @Override
    public abstract PlaybackType getPlaybackType();

    @Override
    public abstract long getDuration();

    public abstract boolean isThirdParty();

    public abstract String getThirdPartyStreamUrl();
}