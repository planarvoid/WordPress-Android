package com.soundcloud.android.api.model;

import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.sync.posts.PostProperty;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.android.tracks.TrackRecordHolder;
import com.soundcloud.java.collections.PropertySet;

public class ApiTrackPost implements PropertySetSource, TrackRecordHolder {

    private final ApiTrack apiTrack;

    public ApiTrackPost(ApiTrack apiTrack) {
        this.apiTrack = apiTrack;
    }

    @Override
    public PropertySet toPropertySet() {
        return apiTrack.toPropertySet()
                .put(PostProperty.IS_REPOST, false)
                .put(PostProperty.CREATED_AT, apiTrack.getCreatedAt());
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
        if (!(o instanceof ApiTrackPost)) {
            return false;
        }
        if (!apiTrack.equals(((ApiTrackPost) o).apiTrack)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return apiTrack.hashCode();
    }
}
