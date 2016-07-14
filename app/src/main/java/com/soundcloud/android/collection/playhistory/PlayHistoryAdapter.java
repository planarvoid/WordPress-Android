package com.soundcloud.android.collection.playhistory;

import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.tracks.TrackItem;

import android.view.View;

import javax.inject.Inject;

class PlayHistoryAdapter extends PagingRecyclerItemAdapter<TrackItem, RecyclerItemAdapter.ViewHolder> {

    @Inject
    PlayHistoryAdapter(PlayHistoryItemRenderer renderer) {
        super(renderer);
    }

    @Override
    protected RecyclerItemAdapter.ViewHolder createViewHolder(View view) {
        return new ViewHolder(view);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return 0;
    }
}
