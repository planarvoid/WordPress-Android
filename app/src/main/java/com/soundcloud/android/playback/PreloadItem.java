package com.soundcloud.android.playback;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import android.os.Parcelable;

@AutoValue
public abstract class PreloadItem implements Parcelable {

    public static PreloadItem create(Urn urn, PlaybackType playbackType) {
        return new AutoValue_PreloadItem(urn.getContent(), playbackType);
    }

    public Urn getUrn() {
        return new Urn(stringUrn());
    }

    protected abstract String stringUrn();

    public abstract PlaybackType getPlaybackType();
}
