package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Category extends ScModel {

    private String mName;
    private List<User> mCreators;


    public String getName() {
        return mName;
    }

    @JsonProperty
    public void setName(String name) {
        this.mName = name;
    }

    public List<User> getCreators() {
        return mCreators;
    }

    public void setCreators(List<User> mCreators) {
        this.mCreators = mCreators;
    }
}