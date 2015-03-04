package com.soundcloud.android.api.model.stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;

import java.util.Date;

public class ApiStreamTrackRepost {

    private final ApiTrack apiTrack;
    private final ApiUser reposter;
    private final long createdAtTime;

    public ApiStreamTrackRepost(@JsonProperty("track") ApiTrack apiTrack,
                                @JsonProperty("reposter") ApiUser reposter,
                                @JsonProperty("created_at") Date createdAt) {
        this.apiTrack = apiTrack;
        this.reposter = reposter;
        this.createdAtTime = createdAt.getTime();
    }

    public ApiTrack getApiTrack() {
        return apiTrack;
    }

    public ApiUser getReposter() {
        return reposter;
    }

    public long getCreatedAtTime() {
        return createdAtTime;
    }
}
