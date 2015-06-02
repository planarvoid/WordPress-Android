package com.soundcloud.android.likes;

import com.soundcloud.android.tracks.DownloadableTrackItemRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.view.adapters.PagingRecyclerViewAdapter;
import com.soundcloud.android.view.adapters.ViewTypes;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

class RecyclerViewPagedTracksAdapter extends PagingRecyclerViewAdapter<TrackItem, RecyclerViewPagedTracksAdapter.ViewHolder> {

    private final TrackItemRenderer trackItemRenderer;

    @Inject
    RecyclerViewPagedTracksAdapter(DownloadableTrackItemRenderer trackItemRenderer) {
        super(trackItemRenderer);
        this.trackItemRenderer = trackItemRenderer;
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return ViewTypes.DEFAULT_VIEW_TYPE;
    }

    TrackItemRenderer getTrackRenderer() {
        return trackItemRenderer;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

}
