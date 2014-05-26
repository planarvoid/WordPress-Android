package com.soundcloud.android.explore;

import java.util.List;

class GenreSection<T> {

    private final int sectionId;
    private final int titleId;
    private final List<T> items;

    public GenreSection(int sectionId, int titleId, List<T> items) {
        this.sectionId = sectionId;
        this.titleId = titleId;
        this.items = items;
    }

    public int getTitleId() {
        return titleId;
    }

    public int getSectionId() {
        return sectionId;
    }

    public List<T> getItems() {
        return items;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GenreSection section = (GenreSection) o;

        if (sectionId != section.sectionId) return false;
        if (!items.equals(section.items)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = sectionId;
        result = 31 * result + items.hashCode();
        return result;
    }
}
