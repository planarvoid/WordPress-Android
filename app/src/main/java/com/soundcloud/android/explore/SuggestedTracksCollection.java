package com.soundcloud.android.explore;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;

import java.util.List;
import java.util.Map;

public class SuggestedTracksCollection extends ModelCollection<ApiTrack> {

    private String trackingTag;

    public SuggestedTracksCollection(@JsonProperty("collection") List<ApiTrack> collection,
                                     @JsonProperty("_links") Map<String, Link> links,
                                     @JsonProperty("query_urn") String queryUrn) {
        super(collection, links, queryUrn);
    }

    public String getTrackingTag() {
        return trackingTag;
    }

    @JsonProperty("tracking_tag")
    public void setTrackingTag(String trackingTag) {
        this.trackingTag = trackingTag;
    }
}
