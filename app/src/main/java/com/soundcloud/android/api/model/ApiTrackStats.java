package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.legacy.model.PlayableStats;

public class ApiTrackStats extends PlayableStats {

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
