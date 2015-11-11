package com.soundcloud.android.playback;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;

import java.util.List;
import java.util.Map;

class RecommendedTracksCollection extends ModelCollection<ApiTrack> {

    private String sourceVersion;

    public RecommendedTracksCollection(@JsonProperty("collection") List<ApiTrack> collection,
                                       @JsonProperty("_links") Map<String, Link> links,
                                       @JsonProperty("query_urn") String queryUrn, String sourceVersion) {
        super(collection, links, queryUrn);
        this.sourceVersion = sourceVersion;
    }

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
