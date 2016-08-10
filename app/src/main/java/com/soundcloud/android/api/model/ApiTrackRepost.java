package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.android.tracks.TrackRecordHolder;
import com.soundcloud.java.collections.PropertySet;

import java.util.Date;

public class ApiTrackRepost implements ApiEntityHolder, TrackRecordHolder {

    private final ApiTrack apiTrack;
    private final Date createdAt;

    @JsonCreator
    public ApiTrackRepost(@JsonProperty("track") ApiTrack apiTrack,
                          @JsonProperty("created_at") Date createdAt) {
        this.apiTrack = apiTrack;
        this.createdAt = createdAt;
    }

    @Override
    public TrackRecord getTrackRecord() {
        return apiTrack;
    }

    @Override
    public PropertySet toPropertySet() {
        return apiTrack.toPropertySet()
                       .put(PostProperty.IS_REPOST, true)
                       .put(PostProperty.CREATED_AT, createdAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApiTrackRepost)) {
            return false;
        }
        ApiTrackRepost that = (ApiTrackRepost) o;
        return apiTrack.equals(that.apiTrack) && createdAt.equals(that.createdAt);
    }

    @Override
    public int hashCode() {
        int result = apiTrack.hashCode();
        result = 31 * result + createdAt.hashCode();
        return result;
    }
}