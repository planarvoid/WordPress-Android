package com.soundcloud.android.model;

import com.google.common.base.Predicate;
import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Category extends ScModel {

    public static final String EXTRA = "category";

    public static Predicate<Category> HAS_USERS_PREDICATE = new Predicate<Category>(){
        @Override
        public boolean apply(Category input) {
            return !input.getUsers().isEmpty();
        }
    };

    private String mKey;
    private List<SuggestedUser> mUsers = Collections.emptyList();

    public enum DisplayType {
        DEFAULT, EMPTY, PROGRESS, ERROR;
    }
    private DisplayType mDisplayType = DisplayType.DEFAULT;

    public Category() { /* for deserialization */ }

    public Category(DisplayType displayType) {
        mDisplayType = displayType;
    }

    public Category(Parcel parcel) {
        super(parcel);
        setKey(parcel.readString());
        mUsers = parcel.readArrayList(SuggestedUser.class.getClassLoader());
        mDisplayType = DisplayType.values()[parcel.readInt()];
    }

    public Category(String urn) {
        super(urn);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mKey);
        dest.writeList(mUsers);
        dest.writeInt(mDisplayType.ordinal());
    }

    public DisplayType getDisplayType() {
        return mDisplayType;
    }

    public void setDisplayType(DisplayType displayType) {
        mDisplayType = displayType;
    }

    public String getKey() {
        return mKey;
    }

    public void setKey(String key) {
        this.mKey = key;
    }

    public String getName(Context context) {
        int resId = context.getResources() .getIdentifier("category_" + mKey, "string", context.getPackageName());
        return resId == 0 ? mKey : context.getString(resId);
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

    public boolean isErrorOrEmpty() {
        return mDisplayType == DisplayType.EMPTY || mDisplayType == DisplayType.ERROR;
    }

    public boolean isError() {
        return mDisplayType == DisplayType.ERROR;
    }

    public boolean isProgressOrEmpty() {
        return mDisplayType == DisplayType.EMPTY || mDisplayType == DisplayType.PROGRESS;
    }

    public String getEmptyMessage(Resources resources) {
        if (isErrorOrEmpty()){
            return resources.getString(isError() ? R.string.suggested_users_section_error : R.string.suggested_users_section_empty);
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
                "mKey='" + mKey + '\'' +
                '}';
    }

    public static final Category progress(){
        return new Category(DisplayType.PROGRESS);
    }

    public static final Category empty() {
        return new Category(DisplayType.EMPTY);
    }

    public static final Category error(){
        return new Category(DisplayType.ERROR);

    }
}