package com.soundcloud.android.cast.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import android.support.annotation.Nullable;

@AutoValue
public abstract class CastMessage {
    @JsonCreator
    public static CastMessage create(@JsonProperty("type") String type, @JsonProperty("payload") CastPlayQueue payload) {
        return new AutoValue_CastMessage(type, payload);
    }

    public static CastMessage create(@JsonProperty("type") String type) {
        return create(type, null);
    }

    @JsonProperty("type")
    public abstract String type();

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("payload")
    public abstract CastPlayQueue payload();
}
