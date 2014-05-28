package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TrackStats extends PlayableStats {

    private int playbackCount;
    private int commentsCount;

    public int getPlaybackCount() {
        return playbackCount;
    }

    @JsonProperty("playback_count")
    public void setPlaybackCount(int playbackCount) {
        this.playbackCount = playbackCount;
    }

    public int getCommentsCount() {
        return commentsCount;
    }

    @JsonProperty("comments_count")
    public void setCommentsCount(int commentsCount) {
        this.commentsCount = commentsCount;
    }
}
