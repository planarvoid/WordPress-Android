package com.soundcloud.android.ads;

import auto.parcel.AutoParcel;
import com.soundcloud.android.playback.PlaybackConstants;

import android.os.Parcelable;

import java.util.Comparator;
import java.util.Locale;

@AutoParcel
public abstract class VideoAdSource implements Parcelable {

    public static final Comparator<VideoAdSource> BITRATE_COMPARATOR = (lhs, rhs) -> Integer.valueOf(lhs.getBitRateKbps()).compareTo(rhs.getBitRateKbps());

    public static VideoAdSource create(ApiVideoSource apiVideoSource) {
        return new AutoParcel_VideoAdSource(
                apiVideoSource.getType(),
                apiVideoSource.getUrl(),
                apiVideoSource.getBitRate(),
                apiVideoSource.getWidth(),
                apiVideoSource.getHeight()
        );
    }

    public abstract String getType();

    public abstract String getUrl();

    public abstract int getBitRateKbps();

    public abstract int getWidth();

    public abstract int getHeight();

    public boolean isMP4() {
        return isOfType(PlaybackConstants.MIME_TYPE_MP4);
    }

    public boolean isHLS() {
        return isOfType(PlaybackConstants.MIME_TYPE_HLS);
    }

    private boolean isOfType(String type) {
        return getType().toLowerCase(Locale.US).equals(type);
    }
}
