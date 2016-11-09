package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.java.functions.Function;

@AutoValue
abstract class ApiAudioAdSource {

    static final Function<ApiAudioAdSource, AudioAdSource> toAudioAdSource =
            new Function<ApiAudioAdSource, AudioAdSource>() {
                @Override
                public AudioAdSource apply(ApiAudioAdSource apiAudioAdSource) {
                    return AudioAdSource.create(apiAudioAdSource);
                }
            };

    @JsonCreator
    public static ApiAudioAdSource create(@JsonProperty("type") String type,
                                          @JsonProperty("url") String url,
                                          @JsonProperty("requires_auth") boolean requiresAuth) {
        return new AutoValue_ApiAudioAdSource(type, url, requiresAuth);
    }

    public abstract String getType();

    public abstract String getUrl();

    public abstract boolean requiresAuth();

}
