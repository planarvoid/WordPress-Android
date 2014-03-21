package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayableStats {

    private long mRepostsCount;
    private long mLikesCount;

    public long getRepostsCount() {
        return mRepostsCount;
    }

    @JsonProperty("reposts_count")
    public void setRepostsCount(long repostsCount) {
        this.mRepostsCount = repostsCount;
    }

    public long getLikesCount() {
        return mLikesCount;
    }

    @JsonProperty("likes_count")
    public void setLikesCount(long likesCount) {
        this.mLikesCount = likesCount;
    }
}
