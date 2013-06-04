package com.soundcloud.android.model;

import java.util.List;

public class CategoryGroup extends ScModel {

    public static final String URN_FACEBOOK = "soundcloud:suggestions:users:social:facebook";
    public static final String URN_MUSIC = "soundcloud:suggested:users:categories:music";
    public static final String URN_SPEECH_AND_SOUNDS = "soundcloud:suggested:users:categories:speech_and_sounds";
    private List<Category> mCategories;

    public List<Category> getCategories() {
        return mCategories;
    }

    public void setCategories(List<Category> categories) {
        mCategories = categories;
    }


}
