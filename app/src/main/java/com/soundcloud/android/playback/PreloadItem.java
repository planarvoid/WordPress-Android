package com.soundcloud.android.playback;

import auto.parcel.AutoParcel;
import com.soundcloud.android.model.Urn;

import android.os.Parcelable;

@AutoParcel
public abstract class PreloadItem implements Parcelable {

    public abstract Urn getUrn();

    public abstract PlaybackType getPlaybackType();
}
