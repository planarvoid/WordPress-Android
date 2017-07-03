package com.soundcloud.android.playback;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import android.os.Parcelable;

@AutoValue
public abstract class PreloadItem implements Parcelable {

    public Urn getUrn() {
        return new Urn(stringUrn());
    }

    protected abstract String stringUrn();

    public abstract PlaybackType getPlaybackType();
}
