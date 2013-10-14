package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SuggestedTracksCollection extends ModelCollection<TrackSummary> {

    private String mTrackingTag;

    public String getTrackingTag() {
        return mTrackingTag;
    }

    @JsonProperty("tracking_tag")
    public void setTrackingTag(String trackingTag) {
        this.mTrackingTag = trackingTag;
    }
}
