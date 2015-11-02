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
        return new AutoParcel_VideoPlaybackItem(adData.getAdUrn(), adData.getVideoSources(), 0, PlaybackType.VIDEO, Consts.NOT_SET);
    }

    public abstract String getAdUrn();

    public abstract List<VideoSource> getSources();

    @Override
    public Urn getTrackUrn() {
       throw new IllegalAccessError("Getting URN from video playback item");
    }

    @Override
    public abstract long getStartPosition();

    @Override
    public abstract PlaybackType getPlaybackType();

    @Override
    public abstract long getDuration();
}
