package com.soundcloud.android.sync.activities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;

import java.util.Date;

public class ApiTrackRepostActivity {

    private final ApiTrack track;
    private final ApiUser reposter;
    private final Date createdAt;

    @JsonCreator
    public ApiTrackRepostActivity(@JsonProperty("track") ApiTrack track,
                                  @JsonProperty("user") ApiUser reposter,
                                  @JsonProperty("created_at") Date createdAt) {
        this.track = track;
        this.reposter = reposter;
        this.createdAt = createdAt;
    }

    public ApiTrack getTrack() {
        return track;
    }

    public ApiUser getReposter() {
        return reposter;
    }

    public Date getCreatedAt() {
        return createdAt;
    }
}
