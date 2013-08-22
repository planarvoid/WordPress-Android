package com.soundcloud.android.adapter;

import com.soundcloud.android.R;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.model.Section;
import com.soundcloud.android.view.adapter.ExploreTracksCategoryRow;
import rx.Observer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class ExploreTracksCategoriesAdapter extends SectionedAdapter<ExploreTracksCategory> {

    public ExploreTracksCategoriesAdapter(Observer<Section<ExploreTracksCategory>> itemObserver) {
        super(itemObserver);
    }

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
