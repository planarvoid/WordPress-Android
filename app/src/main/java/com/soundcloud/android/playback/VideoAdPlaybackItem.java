package com.soundcloud.android.playback;

import android.os.Parcelable;

import com.soundcloud.android.Consts;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.ads.VideoAdSource;
import com.soundcloud.android.model.Urn;

import java.util.List;

import auto.parcel.AutoParcel;

@AutoParcel
public abstract class VideoAdPlaybackItem implements AdPlaybackItem, Parcelable {

    public static VideoAdPlaybackItem create(VideoAd adData, long startPosition) {
        return new AutoParcel_VideoAdPlaybackItem(adData,
                                                  adData.getAdUrn(),
                                                  startPosition,
                                                  PlaybackType.VIDEO_AD,
                                                  Consts.NOT_SET);
    }

    @Override
    public abstract Urn getUrn();

    public List<VideoAdSource> getSources() {
        return ((VideoAd) getAdData()).getVideoSources();
    }

    @Override
    public abstract long getStartPosition();

    @Override
    public abstract PlaybackType getPlaybackType();

    @Override
    public abstract long getDuration();
}
