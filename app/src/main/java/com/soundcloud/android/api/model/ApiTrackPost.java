package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.android.tracks.TrackRecordHolder;

public class ApiTrackPost implements ApiEntityHolder, TrackRecordHolder {

    private final ApiTrack apiTrack;

    @JsonCreator
    public ApiTrackPost(@JsonProperty("track") ApiTrack apiTrack) {
        this.apiTrack = apiTrack;
    }

    @Override
    public TrackRecord getTrackRecord() {
        return apiTrack;
    }

    public ApiTrack getApiTrack() {
        return apiTrack;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApiTrackPost)) {
            return false;
        }
        return apiTrack.equals(((ApiTrackPost) o).apiTrack);
    }

    @Override
    public int hashCode() {
        return apiTrack.hashCode();
    }
}
