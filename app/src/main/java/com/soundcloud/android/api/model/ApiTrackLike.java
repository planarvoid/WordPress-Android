package com.soundcloud.android.api.model;

import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.android.tracks.TrackRecordHolder;
import com.soundcloud.java.collections.PropertySet;

import java.util.Date;

public class ApiTrackLike implements ApiEntityHolder, TrackRecordHolder {

    private final ApiTrack apiTrack;
    private final Date createdAt;

    public ApiTrackLike(ApiTrack apiTrack, Date createdAt) {
        this.apiTrack = apiTrack;
        this.createdAt = createdAt;
    }

    @Override
    public PropertySet toPropertySet() {
        return apiTrack.toPropertySet().put(LikeProperty.CREATED_AT, createdAt);
    }

    @Override
    public TrackRecord getTrackRecord(){
        return apiTrack;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApiTrackLike)) {
            return false;
        }
        return apiTrack.equals(((ApiTrackLike) o).apiTrack);
    }

    @Override
    public int hashCode() {
        return apiTrack.hashCode();
    }
}
