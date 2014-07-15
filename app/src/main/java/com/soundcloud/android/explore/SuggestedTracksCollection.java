package com.soundcloud.android.explore;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;

public class SuggestedTracksCollection extends ModelCollection<ApiTrack> {

    private String trackingTag;

    public String getTrackingTag() {
        return trackingTag;
    }

    @JsonProperty("tracking_tag")
    public void setTrackingTag(String trackingTag) {
        this.trackingTag = trackingTag;
    }
}
