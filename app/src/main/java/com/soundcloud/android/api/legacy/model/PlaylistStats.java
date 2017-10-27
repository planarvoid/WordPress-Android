package com.soundcloud.android.api.legacy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class PlaylistStats {

    public abstract int getRepostsCount();

    public abstract int getLikesCount();

    @JsonCreator
    public static PlaylistStats create(
            @JsonProperty("reposts_count") int repostsCount,
            @JsonProperty("likes_count") int likesCount) {
        return new AutoValue_PlaylistStats(repostsCount, likesCount);
    }

}
