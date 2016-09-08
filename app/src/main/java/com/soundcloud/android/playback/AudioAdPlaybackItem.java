package com.soundcloud.android.playback;

import android.os.Parcelable;

import com.soundcloud.android.Consts;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.AudioAdSource;
import com.soundcloud.android.model.Urn;
import java.util.List;

import auto.parcel.AutoParcel;

@AutoParcel
public abstract class AudioAdPlaybackItem implements PlaybackItem, Parcelable {

    private static final long POSITION_START = 0L;

    public static AudioAdPlaybackItem create(AudioAd audioAd) {
        return new AutoParcel_AudioAdPlaybackItem(
                audioAd.getAdUrn(),
                audioAd.getAudioSources(),
                POSITION_START,
                PlaybackType.AUDIO_AD,
                Consts.NOT_SET
        );
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
