package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class RelatedTracksCollection extends ModelCollection<TrackSummary> {

    private String mSourceVersion;

    public RelatedTracksCollection(List<TrackSummary> collection) {
        super(collection);
    }

    public String getSourceVersion() {
        return mSourceVersion;
    }

    @JsonProperty("source_version")
    public void setSourceVersion(String sourceVersion) {
        mSourceVersion = sourceVersion;
    }
}
