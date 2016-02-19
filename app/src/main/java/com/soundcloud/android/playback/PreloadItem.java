package com.soundcloud.android.playback;

import auto.parcel.AutoParcel;
import com.soundcloud.android.model.Urn;

@AutoParcel
public abstract class PreloadItem {

    public abstract Urn getUrn();

    public abstract PlaybackType getPlaybackType();
}
