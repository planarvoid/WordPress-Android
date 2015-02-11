package com.soundcloud.android.explore;

import com.soundcloud.android.view.adapters.ItemAdapter;

import javax.inject.Inject;


class ExploreGenresAdapter extends ItemAdapter<ExploreGenre> {

    private final GenreCellPresenter cellPresenter;

    @Inject
    ExploreGenresAdapter(GenreCellPresenter cellPresenter) {
        super(cellPresenter);
        this.cellPresenter = cellPresenter;
    }

    @Override
    public void clear() {
        super.clear();
        cellPresenter.clearSections();
    }

    void demarcateSection(GenreSection<ExploreGenre> section) {
        boolean isSectionHeader = true; // true only for the first item in a section
        final int itemCount = items.size();
        for (int i = itemCount - section.getSize(); i < itemCount; i++) {
            cellPresenter.setSectionForPosition(i, section, isSectionHeader);
            isSectionHeader = false;
        }
    }
}
