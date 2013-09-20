package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Stats {

    private long mPlaybackCount;
    private long mRepostsCount;
    private long mLikesCount;
    private long mCommentsCount;

    public long getPlaybackCount() {
        return mPlaybackCount;
    }

    @JsonProperty("playback_count")
    public void setPlaybackCount(long playbackCount) {
        this.mPlaybackCount = playbackCount;
    }

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

    public long getCommentsCount() {
        return mCommentsCount;
    }

    @JsonProperty("comments_count")
    public void setCommentsCount(long commentsCount) {
        this.mCommentsCount = commentsCount;
    }
}
