package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TrackStats extends PlayableStats {

    private long mPlaybackCount;
    private long mCommentsCount;

    public long getPlaybackCount() {
        return mPlaybackCount;
    }

    @JsonProperty("playback_count")
    public void setPlaybackCount(long playbackCount) {
        this.mPlaybackCount = playbackCount;
    }

    public long getCommentsCount() {
        return mCommentsCount;
    }

    @JsonProperty("comments_count")
    public void setCommentsCount(long commentsCount) {
        this.mCommentsCount = commentsCount;
    }
}
