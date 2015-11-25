package com.soundcloud.android.ads;

import auto.parcel.AutoParcel;

@AutoParcel
public abstract class VideoSource {
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