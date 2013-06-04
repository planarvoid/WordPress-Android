package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Category extends ScModel {

    private String mName;
    private String mPermalink;
    private List<User> mUsers;
    private boolean mFollowed;

    public String getPermalink() {
        return mPermalink;
    }

    public void setPermalink(String permalink) {
        this.mPermalink = permalink;
    }

    public String getName() {
        return mName;
    }

    @JsonProperty
    public void setName(String name) {
        this.mName = name;
    }

    public List<User> getUsers() {
        return mUsers;
    }

    public void setUsers(List<User> users) {
        this.mUsers = users;
    }

    public void setFollowed(boolean followed) {
        mFollowed = followed;
    }

    public boolean isFollowed() {
        return mFollowed;
    }
}