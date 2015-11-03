package com.soundcloud.android.activities;

import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.presentation.RecyclerItemAdapter;

import android.view.View;

import javax.inject.Inject;

class ActivitiesAdapter extends PagingRecyclerItemAdapter<ActivityItem, RecyclerItemAdapter.ViewHolder> {

    @Inject
    public ActivitiesAdapter(ActivityItemRenderer itemRenderer) {
        super(itemRenderer);
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return 0;
    }
}
