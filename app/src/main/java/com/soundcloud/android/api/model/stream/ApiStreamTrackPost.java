package com.soundcloud.android.api.model.stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;

import java.util.Date;

public class ApiStreamTrackPost {

    private final ApiTrack apiTrack;
    private final long createdAtTime;

    public ApiStreamTrackPost(@JsonProperty("track") ApiTrack apiTrack, @JsonProperty("created_at") Date createdAt) {
        this.apiTrack = apiTrack;
        this.createdAtTime = createdAt.getTime();
    }

    public ApiTrack getApiTrack() {
        return apiTrack;
    }

    public long getCreatedAtTime() {
        return createdAtTime;
    }
}
