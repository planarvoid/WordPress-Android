package com.soundcloud.android.playback.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;

import java.util.List;

public class RecommendedTracksCollection extends ModelCollection<ApiTrack> {

    private String sourceVersion;

    public RecommendedTracksCollection() {}

    public RecommendedTracksCollection(List<ApiTrack> collection, String sourceVersion) {
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
