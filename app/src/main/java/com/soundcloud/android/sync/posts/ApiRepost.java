package com.soundcloud.android.sync.posts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import java.util.Date;

@AutoValue
public abstract class ApiRepost implements PostRecord {

    @JsonCreator
    public static ApiRepost create(@JsonProperty("target_urn") Urn targetUrn,
                     @JsonProperty("created_at") Date createdAt) {
        return new AutoValue_ApiRepost(targetUrn, createdAt, true);
    }

}
