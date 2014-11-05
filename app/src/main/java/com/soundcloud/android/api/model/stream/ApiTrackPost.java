package com.soundcloud.android.api.model.stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;

public class ApiTrackPost {

    private final ApiTrack apiTrack;

    public ApiTrackPost(@JsonProperty("track") ApiTrack apiTrack) {
        this.apiTrack = apiTrack;
    }

    public ApiTrack getApiTrack() {
        return apiTrack;
    }
}
