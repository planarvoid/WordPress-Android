package com.soundcloud.android.ads;

import auto.parcel.AutoParcel;

import android.os.Parcelable;

import java.util.Comparator;

@AutoParcel
public abstract class VideoSource implements Parcelable {

    public static final Comparator<VideoSource> BITRATE_COMPARATOR = new Comparator<VideoSource>() {
        @Override
        public int compare(VideoSource lhs, VideoSource rhs) {
            return Integer.valueOf(lhs.getBitRate()).compareTo(rhs.getBitRate());
        }
    };

    public static VideoSource create(ApiVideoSource apiVideoSource) {
        return new AutoParcel_VideoSource(
                apiVideoSource.getCodec(),
                apiVideoSource.getUrl(),
                apiVideoSource.getBitRate(),
                apiVideoSource.getWidth(),
                apiVideoSource.getHeight()
        );
    }

    public abstract String getCodec();

    public abstract String getUrl();

    public abstract int getBitRate();

    public abstract int getWidth();

    public abstract int getHeight();
}
