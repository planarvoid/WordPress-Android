package com.soundcloud.android.playback;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.Consts;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.AudioAdSource;
import com.soundcloud.android.model.Urn;

import android.os.Parcelable;

import java.util.List;

@AutoValue
public abstract class AudioAdPlaybackItem implements PlaybackItem, Parcelable {

    private static final long POSITION_START = 0L;

    public static AudioAdPlaybackItem create(AudioAd audioAd) {
        return new AutoValue_AudioAdPlaybackItem(
                audioAd.adUrn(),
                audioAd.audioSources(),
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
