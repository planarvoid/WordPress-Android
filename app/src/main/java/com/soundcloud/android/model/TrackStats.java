package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TrackStats extends PlayableStats {

    private long playbackCount;
    private long commentsCount;

    public long getPlaybackCount() {
        return playbackCount;
    }

    @JsonProperty("playback_count")
    public void setPlaybackCount(long playbackCount) {
        this.playbackCount = playbackCount;
    }

    public long getCommentsCount() {
        return commentsCount;
    }

    @JsonProperty("comments_count")
    public void setCommentsCount(long commentsCount) {
        this.commentsCount = commentsCount;
    }
}
