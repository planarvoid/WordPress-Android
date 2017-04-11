package com.soundcloud.android.ads;

import auto.parcel.AutoParcel;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.playback.PlaybackConstants;

import android.os.Parcelable;

@AutoParcel
public abstract class AudioAdSource implements Parcelable {

    public static AudioAdSource create(ApiModel apiModel) {
        return new AutoParcel_AudioAdSource(apiModel.type(), apiModel.url(), apiModel.requiresAuth());
    }

    public abstract String type();

    public abstract String url();

    public abstract boolean requiresAuth();

    public boolean isMp3() {
        return type().equals(PlaybackConstants.MIME_TYPE_MP3);
    }

    public boolean isHls() {
        return type().equals(PlaybackConstants.MIME_TYPE_HLS);
    }

    @AutoValue
    abstract static class ApiModel {

        @JsonCreator
        public static ApiModel create(@JsonProperty("type") String type,
                                      @JsonProperty("url") String url,
                                      @JsonProperty("requires_auth") boolean requiresAuth) {
            return new AutoValue_AudioAdSource_ApiModel(type, url, requiresAuth);
        }

        public abstract String type();

        public abstract String url();

        public abstract boolean requiresAuth();
    }
}
