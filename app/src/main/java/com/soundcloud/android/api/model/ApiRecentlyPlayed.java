package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ApiRecentlyPlayed {

    @JsonCreator
    public static ApiRecentlyPlayed create(@JsonProperty("played_at") long playedAt,
                                           @JsonProperty("urn") String urn) {
        return new AutoValue_ApiRecentlyPlayed(playedAt, urn);
    }

    @JsonGetter("played_at")
    public abstract long getPlayedAt();

    @JsonGetter("urn")
    public abstract String getUrn();

}
