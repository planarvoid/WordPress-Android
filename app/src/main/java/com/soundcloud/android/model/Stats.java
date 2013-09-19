package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Stats {

    private long playbackCount;
    private long repostsCount;
    private long likesCount;
    private long commentsCount;

    public long getPlaybackCount() {
        return playbackCount;
    }

    @JsonProperty("playback_count")
    public void setPlaybackCount(long playbackCount) {
        this.playbackCount = playbackCount;
    }

    public long getRepostsCount() {
        return repostsCount;
    }

    @JsonProperty("reposts_count")
    public void setRepostsCount(long repostsCount) {
        this.repostsCount = repostsCount;
    }

    public long getLikesCount() {
        return likesCount;
    }

    @JsonProperty("likes_count")
    public void setLikesCount(long likesCount) {
        this.likesCount = likesCount;
    }

    public long getCommentsCount() {
        return commentsCount;
    }

    @JsonProperty("comments_count")
    public void setCommentsCount(long commentsCount) {
        this.commentsCount = commentsCount;
    }
}
