package com.soundcloud.android.view.adapters;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.tracks.DownloadableTrackItemRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

public class PagedTracksRecyclerItemAdapter extends PagingRecyclerItemAdapter<TrackItem, PagedTracksRecyclerItemAdapter.TrackViewHolder> {

    @VisibleForTesting static final int TRACK_ITEM_TYPE = 0;

    private final DownloadableTrackItemRenderer trackRenderer;

    @Inject
    public PagedTracksRecyclerItemAdapter(DownloadableTrackItemRenderer trackRenderer) {
        super(trackRenderer);
        this.trackRenderer = trackRenderer;
    }

    @Override
    public int getBasicItemViewType(int position) {
        return TRACK_ITEM_TYPE;
    }

    public TrackItemRenderer getTrackRenderer() {
        return trackRenderer;
    }

    @Override
    protected TrackViewHolder createViewHolder(View itemView) {
        return new TrackViewHolder(itemView);
    }

    public static class TrackViewHolder extends RecyclerView.ViewHolder {
        public TrackViewHolder(View itemView) {
            super(itemView);
        }
    }

}
