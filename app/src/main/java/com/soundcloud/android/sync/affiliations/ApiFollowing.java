package com.soundcloud.android.sync.affiliations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import java.util.Date;

@AutoValue
public abstract class ApiFollowing {

    @JsonCreator
    public static ApiFollowing create(@JsonProperty("user") Urn userUrn,
                        @JsonProperty("created") Date createdAt,
                        @JsonProperty("target") Urn targetUrn) {
        return new AutoValue_ApiFollowing(targetUrn, userUrn, createdAt);
    }

    public abstract Urn getTargetUrn();

    public abstract Urn getUserUrn();

    public abstract Date getCreatedAt();
}
