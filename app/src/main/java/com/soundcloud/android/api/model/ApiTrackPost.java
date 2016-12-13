package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.android.tracks.TrackRecordHolder;
import com.soundcloud.java.collections.PropertySet;

public class ApiTrackPost implements ApiEntityHolder, TrackRecordHolder {

    private final ApiTrack apiTrack;

    @JsonCreator
    public ApiTrackPost(@JsonProperty("track") ApiTrack apiTrack) {
        this.apiTrack = apiTrack;
    }

    @Override
    public PropertySet toPropertySet() {
        return apiTrack.toPropertySet()
                       .put(PostProperty.IS_REPOST, false)
                       .put(PostProperty.CREATED_AT, apiTrack.getCreatedAt());
    }

    @Override
    public TrackRecord getTrackRecord() {
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