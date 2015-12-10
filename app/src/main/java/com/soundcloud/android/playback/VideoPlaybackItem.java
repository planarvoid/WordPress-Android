package com.soundcloud.android.playback;

import android.os.Parcelable;

import com.soundcloud.android.Consts;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.ads.VideoSource;
import com.soundcloud.android.model.Urn;

import java.util.List;

import auto.parcel.AutoParcel;

@AutoParcel
public abstract class VideoPlaybackItem implements PlaybackItem, Parcelable {

    public static VideoPlaybackItem create(VideoAd adData) {
        return new AutoParcel_VideoPlaybackItem(adData.getAdUrn(), adData.getVideoSources(), 0, PlaybackType.VIDEO_DEFAULT, Consts.NOT_SET);
    }

    @Override
    public abstract Urn getUrn();

    public abstract List<VideoSource> getSources();

    @Override
    public abstract long getStartPosition();

    @Override
    public abstract PlaybackType getPlaybackType();

    @Override
    public abstract long getDuration();
}
