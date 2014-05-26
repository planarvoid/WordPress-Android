package com.soundcloud.android.explore;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.ExploreGenre;
import com.soundcloud.android.view.adapters.CellPresenter;

import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class GenreCellPresenter implements CellPresenter<ExploreGenre, GenreRow> {

    static final int AUDIO_SECTION = 0;
    static final int MUSIC_SECTION = 1;
    private final LayoutInflater layoutInflater;
    private final SparseArray<RowDescriptor> listPositionsToSections;

    @Inject
    GenreCellPresenter(LayoutInflater layoutInflater) {
        this.layoutInflater = layoutInflater;
        this.listPositionsToSections = new SparseArray<RowDescriptor>();
    }

    @Override
    public GenreRow createItemView(int position, ViewGroup parent, int itemViewType) {
        return (GenreRow) layoutInflater.inflate(R.layout.explore_genre_item, parent, false);
    }

    @Override
    public void bindItemView(int position, GenreRow itemView, List<ExploreGenre> genres) {
        RowDescriptor descriptor = listPositionsToSections.get(position);

        if (descriptor.isSectionHeader) {
            itemView.showSectionHeaderWithText(itemView.getResources().getString(descriptor.section.getTitleId()));
        } else {
            itemView.hideSectionHeader();
        }
        String genreTitle = genres.get(position).getTitle();
        itemView.setDisplayName(genreTitle);

        switch (getSection(position).getSectionId()) {
            case AUDIO_SECTION:
                itemView.setTag(Screen.EXPLORE_AUDIO_GENRE.get(genreTitle));
                break;
            case MUSIC_SECTION:
                itemView.setTag(Screen.EXPLORE_MUSIC_GENRE.get(genreTitle));
                break;
            default:
                throw new IllegalArgumentException("Unrecognised genre section, cannot generate screen tag");
        }
    }

    private GenreSection<ExploreGenre> getSection(int position) {
        return listPositionsToSections.get(position).section;
    }

    boolean isSectionHeader(int position) {
        return listPositionsToSections.get(position).isSectionHeader;
    }

    void setSectionForPosition(int position, GenreSection<ExploreGenre> section, boolean isSectionHeader) {
        listPositionsToSections.put(position, new RowDescriptor(section, isSectionHeader));
    }

    void clearSections() {
        listPositionsToSections.clear();
    }

    private static final class RowDescriptor {

        private final GenreSection<ExploreGenre> section;
        private final boolean isSectionHeader;

        RowDescriptor(GenreSection<ExploreGenre> section, boolean isSectionHeader) {
            this.section = section;
            this.isSectionHeader = isSectionHeader;
        }
    }
}
