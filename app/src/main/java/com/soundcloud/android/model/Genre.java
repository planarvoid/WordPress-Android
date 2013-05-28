package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Genre extends ScModel {

    private String mName, mGrouping;

    public String getName() {
        return mName;
    }

    @JsonProperty
    public void setName(String name) {
        this.mName = name;
    }

    public String getGrouping() {
        return mGrouping;
    }

    @JsonProperty
    public void setGrouping(String grouping) {
        this.mGrouping = grouping;
    }
}
