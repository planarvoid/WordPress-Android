package com.soundcloud.android.explore;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ItemAdapter;
import com.soundcloud.android.collections.Section;
import com.soundcloud.android.model.ExploreGenre;
import rx.Observer;

import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;


class ExploreGenresAdapter extends ItemAdapter<ExploreGenre> implements Observer<Section<ExploreGenre>> {

    static final int AUDIO_SECTION = 0;
    static final int MUSIC_SECTION = 1;
    static final int ITEM_VIEW_TYPE_DEFAULT = 0;
    static final int ITEM_VIEW_TYPE_HEADER = 1;
    private static final int INITIAL_LIST_CAPACITY = 30;
    private final LayoutInflater layoutInflater;
    private final SparseArray<RowDescriptor> listPositionsToSections;

    @Inject
    public ExploreGenresAdapter(LayoutInflater layoutInflater) {
        super(ExploreGenresAdapter.INITIAL_LIST_CAPACITY);
        this.layoutInflater = layoutInflater;
        listPositionsToSections = new SparseArray<RowDescriptor>();
    }

    @Override
    public int getItemViewType(int position) {
        return listPositionsToSections.get(position).isSectionHeader ? ITEM_VIEW_TYPE_HEADER : ITEM_VIEW_TYPE_DEFAULT;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public void clear() {
        super.clear();
        listPositionsToSections.clear();
    }

    public Section<ExploreGenre> getSection(int position) {
        return listPositionsToSections.get(position).section;
    }

    @Override
    public void onCompleted() {
        notifyDataSetChanged();
    }

    @Override
    public void onNext(Section<ExploreGenre> section) {
        boolean isSectionHeader = true; // true only for the first item in a section
        for (ExploreGenre item : section.getItems()) {
            RowDescriptor descriptor = new RowDescriptor();
            descriptor.section = section;
            descriptor.isSectionHeader = isSectionHeader;
            isSectionHeader = false;

            addItem(item);
            listPositionsToSections.put(items.size() - 1, descriptor);
        }
    }

    @Override
    public void onError(Throwable t) {
        t.printStackTrace();
    }

    @Override
    protected ExploreGenreCategoryRow createItemView(int position, ViewGroup parent) {
        return (ExploreGenreCategoryRow) layoutInflater.inflate(R.layout.explore_genre_item, parent, false);
    }

    @Override
    protected void bindItemView(int position, View itemView) {
        RowDescriptor descriptor = listPositionsToSections.get(position);
        ExploreGenreCategoryRow categoryRow = (ExploreGenreCategoryRow) itemView;

        if (descriptor.isSectionHeader) {
            categoryRow.showSectionHeaderWithText(itemView.getResources().getString(descriptor.section.getTitleId()));
        } else {
            categoryRow.hideSectionHeader();
        }
        String genreTitle = getItem(position).getTitle();
        categoryRow.setDisplayName(genreTitle);

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

    @VisibleForTesting
    protected static final class RowDescriptor {

        private Section<ExploreGenre> section;
        private boolean isSectionHeader;

    }
}
