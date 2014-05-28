package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayableStats {

    private int repostsCount;
    private int likesCount;

    public int getRepostsCount() {
        return repostsCount;
    }

    @JsonProperty("reposts_count")
    public void setRepostsCount(int repostsCount) {
        this.repostsCount = repostsCount;
    }

    public int getLikesCount() {
        return likesCount;
    }

    @JsonProperty("likes_count")
    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }
}
