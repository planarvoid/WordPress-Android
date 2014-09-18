package com.soundcloud.android.onboarding.suggestions;

import static com.google.common.collect.Collections2.filter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.soundcloud.android.model.ScModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class CategoryGroup extends ScModel {

    public static final String KEY_FACEBOOK = "facebook";
    public static final String KEY_MUSIC = "music";
    public static final String KEY_SPEECH_AND_SOUNDS = "speech_and_sounds";

    @NotNull
    private String mKey;
    @NotNull
    private List<Category> mCategories = Collections.emptyList();

    public CategoryGroup() {
    }

    public CategoryGroup(String key) {
        mKey = key;
    }

    @NotNull
    public List<SuggestedUser> getAllSuggestedUsers() {
        List<SuggestedUser> allUsers = new ArrayList<SuggestedUser>();
        for (Category category : mCategories) {
            allUsers.addAll(category.getUsers());
        }
        return allUsers;
    }

    @VisibleForTesting
    public List<Category> getCategories() {
        return mCategories;
    }

    @NotNull
    public Collection<Category> getNonEmptyCategories() {
        return filter(mCategories, Category.HAS_USERS_PREDICATE);
    }

    public void setCategories(@NotNull List<Category> categories) {
        mCategories = categories;
    }

    public int getCategoryCount() {
        return mCategories.size();
    }

    public String getKey() {
        return mKey;
    }

    public void setKey(String key) {
        this.mKey = key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        CategoryGroup that = (CategoryGroup) o;

        if (!mKey.equals(that.mKey)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + mKey.hashCode();
        return result;
    }

    public static CategoryGroup createProgressGroup(String key) {
        CategoryGroup categoryGroup = new CategoryGroup(key);
        categoryGroup.setCategories(Lists.<Category>newArrayList(Category.progress()));
        return categoryGroup;
    }

    public static CategoryGroup createErrorGroup(String key) {
        CategoryGroup categoryGroup = new CategoryGroup(key);
        categoryGroup.setCategories(Lists.<Category>newArrayList(Category.error()));
        return categoryGroup;
    }

    public boolean isFacebook() {
        return mKey.equals(KEY_FACEBOOK);
    }

    public boolean isEmpty() {
        return getNonEmptyCategories().isEmpty();
    }

    public void removeDuplicateUsers(Set<SuggestedUser> currentUniqueSuggestedUsersSet) {
        for (Category category : mCategories) {
            Iterator<SuggestedUser> iter = category.getUsers().iterator();
            while (iter.hasNext()) {
                final SuggestedUser suggestedUser = iter.next();
                if (currentUniqueSuggestedUsersSet.contains(suggestedUser)) {
                    iter.remove();
                } else {
                    currentUniqueSuggestedUsersSet.add(suggestedUser);
                }
            }
        }
    }
}
