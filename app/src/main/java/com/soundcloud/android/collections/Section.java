package com.soundcloud.android.collections;

import java.util.List;

public class Section<T> {

    private final int mSectionId;
    private final int mTitleId;
    private final List<T> mItems;

    public Section(int sectionId, int titleId, List<T> items) {
        this.mSectionId = sectionId;
        this.mTitleId = titleId;
        this.mItems = items;
    }

    public int getTitleId() {
        return mTitleId;
    }

    public int getSectionId() {
        return mSectionId;
    }

    public List<T> getItems() {
        return mItems;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Section section = (Section) o;

        if (mSectionId != section.mSectionId) return false;
        if (!mItems.equals(section.mItems)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mSectionId;
        result = 31 * result + mItems.hashCode();
        return result;
    }
}
