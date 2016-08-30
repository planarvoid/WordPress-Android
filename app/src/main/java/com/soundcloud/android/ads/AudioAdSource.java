package com.soundcloud.android.ads;

import android.os.Parcelable;

import com.soundcloud.android.playback.PlaybackConstants;

import auto.parcel.AutoParcel;

@AutoParcel
public abstract class AudioAdSource implements Parcelable {

    public static AudioAdSource create(ApiAudioAdSource apiAudioSource) {
        return new AutoParcel_AudioAdSource(
                apiAudioSource.getType(),
                apiAudioSource.getUrl(),
                apiAudioSource.requiresAuth()
        );
    }

    public abstract String getType();

    public abstract String getUrl();

    public abstract boolean requiresAuth();

    public boolean isMp3() {
        return getType().equals(PlaybackConstants.MIME_TYPE_MP3);
    }

    public boolean isHls() {
        return getType().equals(PlaybackConstants.MIME_TYPE_HLS);
    }
}
