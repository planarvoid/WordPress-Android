package com.soundcloud.android.collection;

import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.java.collections.PropertySet;

import java.util.List;

final class LikesItem {

    private final List<LikedTrackPreview> trackPreviews;
    private final PropertySet properties;

    static LikesItem fromTrackPreviews(List<LikedTrackPreview> trackPreviews) {
        return new LikesItem(trackPreviews, PropertySet.create());
    }

    LikesItem(List<LikedTrackPreview> trackPreviews, PropertySet properties) {
        this.trackPreviews = trackPreviews;
        this.properties = properties;
    }

    public void update(PropertySet properties) {
        this.properties.update(properties);
    }

    public List<LikedTrackPreview> getTrackPreviews() {
        return trackPreviews;
    }

    public OfflineState getDownloadState() {
        return properties.getOrElse(OfflineProperty.OFFLINE_STATE, OfflineState.NOT_OFFLINE);
    }

}
