package com.soundcloud.android.adapter;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.model.ExploreTracksCategories;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.view.adapter.ExploreTracksCategoryRow;

import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class ExploreTracksCategoriesAdapter extends ScAdapter<ExploreTracksCategory> {

    @VisibleForTesting
    protected static final int VIEW_TYPE_DEFAULT = 0;
    @VisibleForTesting
    protected static final int VIEW_TYPE_SECTION = 1;

    private static final int INITIAL_LIST_CAPACITY = 30;

    private final SparseIntArray mListPositionsToSectionTitleResIds;

    public ExploreTracksCategoriesAdapter() {
        this(new SparseIntArray());
    }

    protected ExploreTracksCategoriesAdapter(SparseIntArray listPositionsToSectionTitleResIds){
        super(INITIAL_LIST_CAPACITY);
        mListPositionsToSectionTitleResIds = listPositionsToSectionTitleResIds;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return mListPositionsToSectionTitleResIds.get(position, -1) == -1 ? VIEW_TYPE_DEFAULT : VIEW_TYPE_SECTION;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    public void setExploreTracksCategories(ExploreTracksCategories exploreTracksCategories) {
        clear();
        mListPositionsToSectionTitleResIds.clear();

        mItems.addAll(exploreTracksCategories.getMusic());
        mItems.addAll(exploreTracksCategories.getAudio());

        mListPositionsToSectionTitleResIds.put(0, R.string.explore_category_header_music);
        mListPositionsToSectionTitleResIds.put(exploreTracksCategories.getMusic().size(), R.string.explore_category_header_audio);
    }

    @Override
    protected ExploreTracksCategoryRow createItemView(int position, ViewGroup parent) {
        return (ExploreTracksCategoryRow) LayoutInflater.from(parent.getContext()).inflate(R.layout.explore_tracks_category_item, null);
    }

    @Override
    protected void bindItemView(int position, View itemView) {
        ExploreTracksCategoryRow row = (ExploreTracksCategoryRow) itemView;
        row.setDisplayName(getItem(position).getDisplayName(itemView.getContext()));

        // set section header
        int resId = mListPositionsToSectionTitleResIds.get(position, -1);
        if (resId != -1) {
            row.showSectionHeader(itemView.getResources().getString(resId));
        } else {
            row.hideSectionHeader();
        }
    }

}
