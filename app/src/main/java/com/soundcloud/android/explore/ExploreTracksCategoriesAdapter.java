package com.soundcloud.android.explore;

import com.soundcloud.android.R;
import com.soundcloud.android.collections.SectionedAdapter;
import com.soundcloud.android.model.ExploreTracksCategory;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class ExploreTracksCategoriesAdapter extends SectionedAdapter<ExploreTracksCategory> {

    static final int AUDIO_SECTION = 0;
    static final int MUSIC_SECTION = 1;

    @Override
    protected ExploreTracksCategoryRow createItemView(int position, ViewGroup parent) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        return (ExploreTracksCategoryRow) layoutInflater.inflate(R.layout.explore_tracks_category_item, null);
    }

    @Override
    protected void bindItemView(int position, View itemView) {
        super.bindItemView(position, itemView);
        ((ExploreTracksCategoryRow) itemView).setDisplayName(getItem(position).getTitle());
    }
}
