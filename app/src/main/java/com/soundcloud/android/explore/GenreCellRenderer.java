package com.soundcloud.android.explore;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.view.adapters.CellRenderer;

import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;

class GenreCellRenderer implements CellRenderer<ExploreGenre> {

    static final int AUDIO_SECTION = 0;
    static final int MUSIC_SECTION = 1;
    private final SparseArray<RowDescriptor> listPositionsToSections;

    @Inject
    GenreCellRenderer() {
        this.listPositionsToSections = new SparseArray<RowDescriptor>();
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.explore_genre_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<ExploreGenre> genres) {
        RowDescriptor descriptor = listPositionsToSections.get(position);

        TextView categoryTitle = (TextView) itemView.findViewById(android.R.id.text1);
        TextView sectionHeader = (TextView) itemView.findViewById(R.id.list_section_header);

        updateSectionHeader(itemView, descriptor, sectionHeader);
        String genreTitle = genres.get(position).getTitle();
        categoryTitle.setText(genreTitle);

        setTrackingTag(position, itemView, genreTitle);
    }

    private void updateSectionHeader(View itemView, RowDescriptor descriptor, TextView sectionHeader) {
        if (descriptor.isSectionHeader) {
            final String headerTitle = itemView.getResources().getString(descriptor.section.getTitleId());
            sectionHeader.setText(headerTitle.toUpperCase(Locale.getDefault()));
            sectionHeader.setVisibility(View.VISIBLE);
        } else {
            sectionHeader.setVisibility(View.GONE);
        }
    }

    private void setTrackingTag(int position, View itemView, String genreTitle) {
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
