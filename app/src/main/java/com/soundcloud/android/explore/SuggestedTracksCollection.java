package com.soundcloud.android.explore;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;

import java.util.List;
import java.util.Map;

public class SuggestedTracksCollection extends ModelCollection<ApiTrack> {

    private final String trackingTag;

    public SuggestedTracksCollection(@JsonProperty("collection") List<ApiTrack> collection,
                                     @JsonProperty("_links") Map<String, Link> links,
                                     @JsonProperty("query_urn") String queryUrn,
                                     @JsonProperty("tracking_tag") String trackingTag) {
        super(collection, links, queryUrn);
        this.trackingTag = trackingTag;
    }

    public String getTrackingTag() {
        return trackingTag;
    }

}
