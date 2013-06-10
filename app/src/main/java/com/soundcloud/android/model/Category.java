package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;
import java.util.Set;

public class Category extends ScModel {

    public static final Category EMPTY = new EmptyCategory();
    public static final Category PROGRESS = new ProgressCategory();
    public static String EXTRA = "category";

    private String mName;
    private String mPermalink;
    private List<SuggestedUser> mUsers;


    public Category() { /* for deserialization */ }

    public Category(Parcel parcel) {
        super(parcel);
        setName(parcel.readString());
        setPermalink(parcel.readString());
        mUsers = parcel.readArrayList(SuggestedUser.class.getClassLoader());
    }

    public Category(String urn) {
        super(urn);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mName);
        dest.writeString(mPermalink);
        dest.writeList(mUsers);
    }

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

    public boolean isFollowed(Set<Long> userFollowings) {
        for (SuggestedUser user : mUsers) {
            if (userFollowings.contains(user.getId())) return true;
        }
        return false;
    }

    public List<SuggestedUser> getNotFollowedUsers(Set<Long> userFollowings){
        return getUsersByFollowStatus(userFollowings, false);
    }

    public List<SuggestedUser> getFollowedUsers(Set<Long> userFollowings){
        return getUsersByFollowStatus(userFollowings, true);
    }

    private List<SuggestedUser> getUsersByFollowStatus(Set<Long> userFollowings, boolean isFollowing) {
        List<SuggestedUser> resultSuggestedUsers = Lists.newArrayList();
        for (SuggestedUser user : mUsers) {
            final boolean contains = userFollowings.contains(user.getId());
            if ((isFollowing && contains) || (!isFollowing && !contains)) resultSuggestedUsers.add(user);
        }
        return resultSuggestedUsers;
    }

    public static final Parcelable.Creator<Category> CREATOR = new Parcelable.Creator<Category>() {
        public Category createFromParcel(Parcel in) {
            return new Category(in);
        }
        public Category[] newArray(int size) {
            return new Category[size];
        }
    };

    @Override
    public String toString() {
        return "Category{" +
                "mPermalink='" + mPermalink + '\'' +
                '}';
    }

    private static final class EmptyCategory extends Category {}
    private static final class ProgressCategory extends Category {}
}