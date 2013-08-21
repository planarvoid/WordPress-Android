package com.soundcloud.android.model;

import java.util.Collections;
import java.util.List;

public class Section<T> {

    private int mTitleId;
    private List<T> mItems = Collections.emptyList();

    public Section(int titleId, List<T> items) {
        this.mTitleId = titleId;
        this.mItems = items;
    }

    public int getTitleId() {
        return mTitleId;
    }

    public List<T> getItems() {
        return mItems;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Section section = (Section) o;

        if (mTitleId != section.mTitleId) return false;
        if (!mItems.equals(section.mItems)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mTitleId;
        result = 31 * result + mItems.hashCode();
        return result;
    }
}
