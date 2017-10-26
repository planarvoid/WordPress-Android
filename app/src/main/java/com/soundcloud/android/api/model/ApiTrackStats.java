package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ApiTrackStats {

    public abstract int getPlaybackCount();

    public abstract int getCommentsCount();

    public abstract int getRepostsCount();

    public abstract int getLikesCount();

    @JsonCreator
    public static ApiTrackStats create(
            @JsonProperty("playback_count") int newPlaybackCount,
            @JsonProperty("comments_count") int newCommentsCount,
            @JsonProperty("reposts_count") int newRepostsCount,
            @JsonProperty("likes_count") int newLikesCount) {
        return builder()
                .playbackCount(newPlaybackCount)
                .commentsCount(newCommentsCount)
                .repostsCount(newRepostsCount)
                .likesCount(newLikesCount)
                .build();
    }

    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_ApiTrackStats.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder playbackCount(int newPlaybackCount);

        public abstract Builder commentsCount(int newCommentsCount);

        public abstract Builder repostsCount(int newRepostsCount);

        public abstract Builder likesCount(int newLikesCount);

        public abstract ApiTrackStats build();
    }
}
