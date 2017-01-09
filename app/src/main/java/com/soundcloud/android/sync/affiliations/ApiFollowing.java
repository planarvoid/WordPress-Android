package com.soundcloud.android.sync.affiliations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.functions.Function;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

@AutoValue
public abstract class ApiFollowing {

    static final Function<ApiFollowing, Long> TO_USER_IDS = input -> input.getTargetUrn().getNumericId();

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
