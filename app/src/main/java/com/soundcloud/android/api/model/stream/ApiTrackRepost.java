package com.soundcloud.android.api.model.stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;

import java.util.Date;

public class ApiTrackRepost {

    private final ApiTrack apiTrack;
    private final ApiUser reposter;
    private final Date createdAt;

    public ApiTrackRepost(@JsonProperty("track") ApiTrack apiTrack,
                          @JsonProperty("reposter") ApiUser reposter,
                          @JsonProperty("created_at") Date createdAt) {
        this.apiTrack = apiTrack;
        this.reposter = reposter;
        this.createdAt = createdAt;
    }

    public ApiTrack getApiTrack() {
        return apiTrack;
    }

    public ApiUser getReposter() {
        return reposter;
    }

    public Date getCreatedAt() {
        return createdAt;
    }
}
