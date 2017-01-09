package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.java.functions.Function;

@AutoValue
public abstract class ApiVideoSource {
    public static Function<ApiVideoSource, VideoAdSource> toVideoAdSource = apiVideoSource -> VideoAdSource.create(apiVideoSource);

    @JsonCreator
    public static ApiVideoSource create(@JsonProperty("type") String type,
                                        @JsonProperty("url") String url,
                                        @JsonProperty("bitrate_kbps") int bitRate,
                                        @JsonProperty("width") int width,
                                        @JsonProperty("height") int height) {
        return new AutoValue_ApiVideoSource(type, url, bitRate, width, height);
    }

    public abstract String getType();

    public abstract String getUrl();

    public abstract int getBitRate();

    public abstract int getWidth();

    public abstract int getHeight();
}
