package com.soundcloud.android.collections;

import com.soundcloud.android.R;

import java.util.List;

public class Section<T> {

    public static final int AUDIO = 0;
    public static final int MUSIC = 1;

    private final int mSectionId;
    private final List<T> mItems;

    public static <T> Section<T> music(List<T> items) {
        return new Section<T>(MUSIC, items);
    }

    public static <T> Section<T> audio(List<T> items) {
        return new Section<T>(AUDIO, items);
    }

    private Section(int sectionId, List<T> items) {
        this.mSectionId = sectionId;
        this.mItems = items;
    }

    public int getTitleId() {
        return mSectionId == AUDIO ? R.string.explore_category_header_audio : R.string.explore_category_header_music;
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
