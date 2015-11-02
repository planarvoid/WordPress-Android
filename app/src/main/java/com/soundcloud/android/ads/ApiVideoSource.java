package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiVideoSource {
    public final String codec;
    public final String url;
    public final int bitRate;
    public final int width;
    public final int height;

    @JsonCreator
    public ApiVideoSource(@JsonProperty("codec") String codec,
                          @JsonProperty("url") String url,
                          @JsonProperty("bitrate_kbps") int bitRate,
                          @JsonProperty("width") int width,
                          @JsonProperty("height") int height) {
        this.codec = codec;
        this.url = url;
        this.bitRate = bitRate;
        this.width = width;
        this.height = height;
    }
}
