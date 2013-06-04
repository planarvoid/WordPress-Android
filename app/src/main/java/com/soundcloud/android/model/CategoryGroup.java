package com.soundcloud.android.model;

import java.util.List;

public class CategoryGroup extends ScModel {

    private List<Category> category;
    private String mCategoryGroupName;

    public List<Category> setCategory() {
        return category;
    }

    public void setCategory(List<Category> category) {
        this.category = category;
    }

    public String getCategoryGroupName() {
        return mCategoryGroupName;
    }

    public void setCategoryGroupName(String groupName) {
        this.mCategoryGroupName = groupName;
    }
}
