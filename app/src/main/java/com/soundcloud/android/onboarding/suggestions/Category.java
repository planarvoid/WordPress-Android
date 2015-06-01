package com.soundcloud.android.onboarding.suggestions;

import com.google.common.base.Predicate;
import com.soundcloud.android.R;
import com.soundcloud.android.model.ScModel;

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
    public static final Predicate<Category> HAS_USERS_PREDICATE = new Predicate<Category>() {
        @Override
        public boolean apply(Category input) {
            final DisplayType displayType = input.getDisplayType();
            return displayType == DisplayType.PROGRESS || displayType == DisplayType.ERROR || !input.getUsers().isEmpty();
        }
    };
    public static final Parcelable.Creator<Category> CREATOR = new Parcelable.Creator<Category>() {
        public Category createFromParcel(Parcel in) {
            return new Category(in);
        }

        public Category[] newArray(int size) {
            return new Category[size];
        }
    };
    private static final String FACEBOOK_FRIENDS = "facebook_friends";
    private static final String FACEBOOK_LIKES = "facebook_likes";
    private String key;
    private List<SuggestedUser> users = Collections.emptyList();
    private DisplayType displayType = DisplayType.DEFAULT;

    public Category() { /* for deserialization */ }

    public Category(DisplayType displayType) {
        this.displayType = displayType;
    }

    public Category(Parcel parcel) {
        super(parcel);
        key = parcel.readString();
        users = parcel.readArrayList(SuggestedUser.class.getClassLoader());
        displayType = DisplayType.values()[parcel.readInt()];
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(key);
        dest.writeList(users);
        dest.writeInt(displayType.ordinal());
    }

    public DisplayType getDisplayType() {
        return displayType;
    }

    public void setDisplayType(DisplayType displayType) {
        this.displayType = displayType;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName(Context context) {
        int resId = context.getResources().getIdentifier("category_" + key, "string", context.getPackageName());
        return resId == 0 ? key : context.getString(resId);
    }

    public List<SuggestedUser> getUsers() {
        return users;
    }

    public void setUsers(List<SuggestedUser> users) {
        this.users = users;
    }

    public boolean isFollowed(Set<Long> userFollowings) {
        for (SuggestedUser user : users) {
            if (userFollowings.contains(user.getId())) {
                return true;
            }
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
        return displayType == DisplayType.EMPTY || displayType == DisplayType.ERROR;
    }

    public boolean isError() {
        return displayType == DisplayType.ERROR;
    }

    public boolean isProgressOrEmpty() {
        return displayType == DisplayType.EMPTY || displayType == DisplayType.PROGRESS;
    }

    public boolean isFacebookCategory() {
        // Note! These keys are defined by the server (but they shouldn't change)
        return key.equals(FACEBOOK_FRIENDS) || key.equals(FACEBOOK_LIKES);
    }

    public String getEmptyMessage(Resources resources) {
        if (isErrorOrEmpty()) {
            return resources.getString(isError() ? R.string.suggested_users_section_error : R.string.suggested_users_section_empty);
        }
        return null;
    }

    @Override
    public String toString() {
        return "Category{" +
                "key='" + key + '\'' +
                '}';
    }

    public static Category progress() {
        return new Category(DisplayType.PROGRESS);
    }

    public static Category empty() {
        return new Category(DisplayType.EMPTY);
    }

    public static Category error() {
        return new Category(DisplayType.ERROR);
    }

    private List<SuggestedUser> getUsersByFollowStatus(Set<Long> userFollowings, boolean isFollowing) {
        List<SuggestedUser> resultSuggestedUsers = new ArrayList(userFollowings.size());
        for (SuggestedUser user : users) {
            final boolean contains = userFollowings.contains(user.getId());
            if ((isFollowing && contains) || (!isFollowing && !contains)) {
                resultSuggestedUsers.add(user);
            }
        }
        return resultSuggestedUsers;
    }

    public enum DisplayType {
        DEFAULT, EMPTY, PROGRESS, ERROR;
    }
}
