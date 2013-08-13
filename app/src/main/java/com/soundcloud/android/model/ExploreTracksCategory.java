package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import android.content.Context;

import java.util.Collections;
import java.util.Map;

public class ExploreTracksCategory {

    private String mKey;
    private Map<String, Link> mLinks = Collections.emptyMap();

    public ExploreTracksCategory(){ }

    public ExploreTracksCategory(String key) {
        this.mKey = key;
    }

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

    public String getDisplayName(Context context) {
        int resId = context.getResources().getIdentifier("explore_category_" + mKey, "string", context.getPackageName());
        return resId == 0 ? mKey.replace("_", " ") : context.getString(resId);
    }
}
