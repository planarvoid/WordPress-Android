package com.soundcloud.android.onboarding.suggestions;

import com.soundcloud.android.api.legacy.model.ScModel;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.MoreCollections;
import org.jetbrains.annotations.NotNull;

import android.support.annotation.VisibleForTesting;

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

    private String key;
    private List<Category> categories = Collections.emptyList();

    public CategoryGroup() {
    }

    public CategoryGroup(String key) {
        this.key = key;
    }

    @NotNull
    public List<SuggestedUser> getAllSuggestedUsers() {
        List<SuggestedUser> allUsers = new ArrayList<>();
        for (Category category : categories) {
            allUsers.addAll(category.getUsers());
        }
        return allUsers;
    }

    @VisibleForTesting
    public List<Category> getCategories() {
        return categories;
    }

    @NotNull
    public Collection<Category> getNonEmptyCategories() {
        return MoreCollections.filter(categories, Category.HAS_USERS_PREDICATE);
    }

    public void setCategories(@NotNull List<Category> categories) {
        this.categories = categories;
    }

    public int getCategoryCount() {
        return categories.size();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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

        if (!key.equals(that.key)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + key.hashCode();
        return result;
    }

    public static CategoryGroup createProgressGroup(String key) {
        CategoryGroup categoryGroup = new CategoryGroup(key);
        categoryGroup.setCategories(Lists.newArrayList(Category.progress()));
        return categoryGroup;
    }

    public static CategoryGroup createErrorGroup(String key) {
        CategoryGroup categoryGroup = new CategoryGroup(key);
        categoryGroup.setCategories(Lists.newArrayList(Category.error()));
        return categoryGroup;
    }

    public boolean isFacebook() {
        return key.equals(KEY_FACEBOOK);
    }

    public boolean isEmpty() {
        return getNonEmptyCategories().isEmpty();
    }

    public void removeDuplicateUsers(Set<SuggestedUser> currentUniqueSuggestedUsersSet) {
        for (Category category : categories) {
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
