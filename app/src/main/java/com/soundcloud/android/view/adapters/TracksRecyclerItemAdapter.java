package com.soundcloud.android.view.adapters;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.tracks.TrackItem;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public class TracksRecyclerItemAdapter extends RecyclerItemAdapter<TrackItem, TracksRecyclerItemAdapter.TrackViewHolder>
        implements PlayingTrackAware, RepeatableItemAdapter {

    private static final int TRACK_ITEM_TYPE = 0;

    public TracksRecyclerItemAdapter(CellRenderer<TrackItem> cellRenderer) {
        super(cellRenderer);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return TRACK_ITEM_TYPE;
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingUrn) {
        for (int position = 0; position < getItemCount(); position++) {
            TrackItem item = getItem(position);
            boolean isCurrent = item.getUrn().equals(currentlyPlayingUrn);
            updateNowPlayingItem(position, item, isCurrent);
        }
    }

    public void updateNowPlaying(int currentPosition) {
        for (int position = 0; position < getItemCount(); position++) {
            boolean isCurrent = currentPosition == position;
            updateNowPlayingItem(position, getItem(position), isCurrent);
        }
    }

    private void updateNowPlayingItem(int position, TrackItem item, boolean isCurrent) {
        if (item.isPlaying() || isCurrent) {
            item.setIsPlaying(isCurrent);
            notifyItemChanged(position);
        }
    }

    @Override
    public void updateInRepeatMode(boolean isInRepeatMode) {
        for (int position = 0; position < getItemCount(); position++) {
            final TrackItem item = getItem(position);
            if (item.isInRepeatMode() != isInRepeatMode) {
                item.setInRepeatMode(isInRepeatMode);
                if (!item.isPlaying()) {
                    notifyItemChanged(position);
                }
            }
        }
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
