package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class RelatedTracksCollection extends ModelCollection<TrackSummary> {

    private String mSourceVersion;

    public RelatedTracksCollection() {}

    public RelatedTracksCollection(List<TrackSummary> collection, String sourceVersion) {
        super(collection);
        mSourceVersion = sourceVersion;
    }

    public String getSourceVersion() {
        return mSourceVersion;
    }

    @JsonProperty("source_version")
    public void setSourceVersion(String sourceVersion) {
        mSourceVersion = sourceVersion;
    }
}
