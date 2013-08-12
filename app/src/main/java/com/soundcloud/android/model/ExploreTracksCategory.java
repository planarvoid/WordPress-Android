package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

public class ExploreTracksCategory {

    private String mKey;
    private Map<String, Link> mLinks = Collections.emptyMap();


    public String getKey() {
        return mKey;
    }

    public void setKey(String key) {
        this.mKey = key;
    }

    @JsonProperty("_links")
    public Map<String, Link> getLinks() {
        return mLinks;
    }

    public void setLinks(Map<String, Link> links) {
        this.mLinks = links;
    }
}
