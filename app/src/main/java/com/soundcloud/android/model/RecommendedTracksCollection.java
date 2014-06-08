package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class RecommendedTracksCollection extends ModelCollection<TrackSummary> {

    private String sourceVersion;

    public RecommendedTracksCollection() {}

    public RecommendedTracksCollection(List<TrackSummary> collection, String sourceVersion) {
        super(collection);
        this.sourceVersion = sourceVersion;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    @JsonProperty("source_version")
    public void setSourceVersion(String sourceVersion) {
        this.sourceVersion = sourceVersion;
    }
}
