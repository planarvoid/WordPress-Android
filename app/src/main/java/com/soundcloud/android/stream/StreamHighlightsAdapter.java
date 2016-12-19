package com.soundcloud.android.stream;

import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;

import android.view.View;

import javax.inject.Inject;

class StreamHighlightsAdapter extends RecyclerItemAdapter<TrackItem, RecyclerItemAdapter.ViewHolder> {

    @Inject
    public StreamHighlightsAdapter(TrackItemRenderer cellRenderer) {
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
