package com.soundcloud.android.ads;

import auto.parcel.AutoParcel;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.playback.PlaybackConstants;

import android.os.Parcelable;

import java.util.Comparator;
import java.util.Locale;

@AutoParcel
public abstract class VideoAdSource implements Parcelable {

    public static final Comparator<VideoAdSource> BITRATE_COMPARATOR = (lhs, rhs) -> Integer.valueOf(lhs.bitRateKbps()).compareTo(rhs.bitRateKbps());

    public static VideoAdSource create(ApiModel apiModel) {
        return new AutoParcel_VideoAdSource(
                apiModel.type(),
                apiModel.url(),
                apiModel.bitRate(),
                apiModel.width(),
                apiModel.height()
        );
    }

    public abstract String type();

    public abstract String url();

    public abstract int bitRateKbps();

    public abstract int width();

    public abstract int height();

    public boolean isMP4() {
        return isOfType(PlaybackConstants.MIME_TYPE_MP4);
    }

    public boolean isHLS() {
        return isOfType(PlaybackConstants.MIME_TYPE_HLS);
    }

    private boolean isOfType(String type) {
        return type().toLowerCase(Locale.US).equals(type);
    }

    @AutoValue
    public abstract static class ApiModel {
        @JsonCreator
        public static ApiModel create(@JsonProperty("type") String type,
                                      @JsonProperty("url") String url,
                                      @JsonProperty("bitrate_kbps") int bitRate,
                                      @JsonProperty("width") int width,
                                      @JsonProperty("height") int height) {
            return new AutoValue_VideoAdSource_ApiModel(type, url, bitRate, width, height);
        }

        public abstract String type();

        public abstract String url();

        public abstract int bitRate();

        public abstract int width();

        public abstract int height();
    }
}
