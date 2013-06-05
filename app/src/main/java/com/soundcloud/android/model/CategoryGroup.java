package com.soundcloud.android.model;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class CategoryGroup extends ScModel {

    public static final String URN_FACEBOOK = "soundcloud:suggestions:users:social:facebook";
    public static final String URN_MUSIC = "soundcloud:suggested:users:categories:music";
    public static final String URN_SPEECH_AND_SOUNDS = "soundcloud:suggested:users:categories:speech_and_sounds";

    @NotNull private List<Category> mCategories = Collections.emptyList();

    public CategoryGroup() {
    }

    public CategoryGroup(String urn) {
        super(urn);
    }

    @NotNull
    public List<Category> getCategories() {
        return mCategories;
    }

    public void setCategories(@NotNull List<Category> categories) {
        mCategories = categories;
    }

    public int getCategoryCount() {
        return mCategories.size();
    }

}
