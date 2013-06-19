package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.R;

import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Category extends ScModel {

    public static final String EXTRA = "category";

    private String mName;
    private String mPermalink;
    private List<SuggestedUser> mUsers = Collections.emptyList();

    public enum Type {
        DEFAULT, EMPTY, PROGRESS, ERROR;
    }
    private Type mType = Type.DEFAULT;

    public Category() { /* for deserialization */ }

    public Category(Type type) {
        mType = type;
    }

    public Category(Parcel parcel) {
        super(parcel);
        setName(parcel.readString());
        setPermalink(parcel.readString());
        mUsers = parcel.readArrayList(SuggestedUser.class.getClassLoader());
        mType = Type.values()[parcel.readInt()];
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
        dest.writeInt(mType.ordinal());
    }

    public Type getType() {
        return mType;
    }

    public void setType(Type type) {
        mType = type;
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

    public List<SuggestedUser> getNotFollowedUsers(Set<Long> userFollowings) {
        return getUsersByFollowStatus(userFollowings, false);
    }

    public List<SuggestedUser> getFollowedUsers(Set<Long> userFollowings) {
        return getUsersByFollowStatus(userFollowings, true);
    }

    public boolean isErrorOrEmptyCategory() {
        return mType == Type.EMPTY || mType == Type.ERROR;
    }

    public boolean isProgressCategory() {
        return mType == Type.PROGRESS;
    }

    public String getEmptyMessage(Resources resources) {
        if (isErrorOrEmptyCategory()){
            return resources.getString(mType == Type.EMPTY ? R.string.suggested_users_section_empty : R.string.suggested_users_section_error);
        }
        return null;
    }

    private List<SuggestedUser> getUsersByFollowStatus(Set<Long> userFollowings, boolean isFollowing) {
        List<SuggestedUser> resultSuggestedUsers = new ArrayList(userFollowings.size());
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

    public static final Category progress(){
        return new Category(Type.PROGRESS);
    }

    public static final Category empty() {
        return new Category(Type.EMPTY);
    }

    public static final Category error(){
        return new Category(Type.ERROR);

    }
}