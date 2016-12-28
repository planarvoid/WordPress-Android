package com.soundcloud.android.sync.likes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import java.util.Date;

@AutoValue
abstract class ApiDeletedLike implements LikeRecord {
    @JsonCreator
    public static ApiDeletedLike create(@JsonProperty("target_urn") Urn targetUrn) {
        return new AutoValue_ApiDeletedLike(targetUrn, new Date());
    }
}
