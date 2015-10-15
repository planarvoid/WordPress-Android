package com.soundcloud.android.view.adapters;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.tracks.TrackItem;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

public class TracksRecyclerItemAdapter extends RecyclerItemAdapter<TrackItem, TracksRecyclerItemAdapter.TrackViewHolder>
        implements NowPlayingAdapter {

    private static final int TRACK_ITEM_TYPE = 0;

    @Inject
    public TracksRecyclerItemAdapter() {
    }

    @Override
    public int getBasicItemViewType(int position) {
        return TRACK_ITEM_TYPE;
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingUrn) {
        for (TrackItem item : getItems()) {
            item.setIsPlaying(item.getEntityUrn().equals(currentlyPlayingUrn));
        }
        notifyDataSetChanged();
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
