package com.soundcloud.android.stations;

import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.presentation.RecyclerItemAdapter;

import android.view.View;

class StationTracksAdapter extends PagingRecyclerItemAdapter<StationInfoTrack, RecyclerItemAdapter.ViewHolder> {

    StationTracksAdapter(StationTrackRenderer cellRenderer) {
        super(cellRenderer);
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
