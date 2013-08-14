package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.behavior.InSection;
import com.soundcloud.android.model.behavior.Titled;

import android.content.Context;

import java.util.Collections;
import java.util.Map;

public class ExploreTracksCategory implements InSection {

    private String mKey;
    private Map<String, Link> mLinks = Collections.emptyMap();
    private ExploreTracksCategorySection mSection;

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
        // TODO cache name?
        int resId = context.getResources().getIdentifier("explore_category_" + mKey, "string", context.getPackageName());
        return resId == 0 ? mKey.replace("_", " ") : context.getString(resId);
    }

    public Titled getSection() {
        return mSection;
    }

    public void setSection(ExploreTracksCategorySection section) {
        this.mSection = section;
    }
}
