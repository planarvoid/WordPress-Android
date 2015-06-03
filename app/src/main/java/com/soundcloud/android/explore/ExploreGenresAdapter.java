package com.soundcloud.android.explore;

import com.soundcloud.android.presentation.ListItemAdapter;

import javax.inject.Inject;


class ExploreGenresAdapter extends ListItemAdapter<ExploreGenre> {

    private final GenreCellRenderer cellRenderer;

    @Inject
    ExploreGenresAdapter(GenreCellRenderer cellRenderer) {
        super(cellRenderer);
        this.cellRenderer = cellRenderer;
    }

    @Override
    public void clear() {
        super.clear();
        cellRenderer.clearSections();
    }

    void demarcateSection(GenreSection<ExploreGenre> section) {
        boolean isSectionHeader = true; // true only for the first item in a section
        final int itemCount = items.size();
        for (int i = itemCount - section.getSize(); i < itemCount; i++) {
            cellRenderer.setSectionForPosition(i, section, isSectionHeader);
            isSectionHeader = false;
        }
    }
}
