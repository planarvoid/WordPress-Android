package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Set;

public class Category extends ScModel {

    public static final Category EMPTY = new EmptyCategory();
    public static final Category PROGRESS = new ProgressCategory();

    private String mName;
    private String mPermalink;
    private List<SuggestedUser> mUsers;
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

    public List<SuggestedUser> getUsers() {
        return mUsers;
    }

    public void setUsers(List<SuggestedUser> users) {
        this.mUsers = users;
    }

    public void setFollowed(boolean followed) {
        mFollowed = followed;
    }

    public boolean isFollowed() {
        return mFollowed;
    }

    public List<SuggestedUser> getFollowedUsers(Set<Long> userFollowings) {
        List<SuggestedUser> following = Lists.newArrayList();
        for (SuggestedUser user : mUsers) {
            if (userFollowings.contains(user.getId())) following.add(user);
        }
        return following;
    }

    private static final class EmptyCategory extends Category {}
    private static final class ProgressCategory extends Category {}
}