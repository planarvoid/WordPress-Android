package com.soundcloud.android.sync.activities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.comments.ApiComment;

import java.util.Date;

public class ApiTrackComment {

    private final ApiTrack track;
    private final ApiComment comment;
    private final Date createdAt;

    @JsonCreator
    public ApiTrackComment(@JsonProperty("track") ApiTrack track,
                           @JsonProperty("comment") ApiComment comment,
                           @JsonProperty("created_at") Date createdAt) {
        this.track = track;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public ApiTrack getTrack() {
        return track;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public ApiComment getComment() {
        return comment;
    }
}
