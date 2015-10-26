package com.soundcloud.android.sync.activities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;

import java.util.Date;

public class ApiTrackLikeActivity {

    private final ApiTrack track;
    private final ApiUser user;
    private final Date createdAt;

    @JsonCreator
    public ApiTrackLikeActivity(@JsonProperty("track") ApiTrack track,
                                @JsonProperty("user") ApiUser user,
                                @JsonProperty("created_at") Date createdAt) {
        this.track = track;
        this.user = user;
        this.createdAt = createdAt;
    }

    public ApiTrack getTrack() {
        return track;
    }

    public ApiUser getUser() {
        return user;
    }

    public Date getCreatedAt() {
        return createdAt;
    }
}
