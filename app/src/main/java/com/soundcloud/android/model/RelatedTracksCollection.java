package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RelatedTracksCollection extends ModelCollection<TrackSummary> {

    private String mSourceVersion;

    public String getSourceVersion() {
        return mSourceVersion;
    }

    @JsonProperty("source_version")
    public void setSourceVersion(String sourceVersion) {
        mSourceVersion = sourceVersion;
    }
}
