package com.soundcloud.android.view.adapters;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

public class TracksRecyclerItemAdapter extends RecyclerItemAdapter<TrackItem, TracksRecyclerItemAdapter.TrackViewHolder>
        implements PlayingTrackAware, RepeatableItemAdapter {

    private static final int TRACK_ITEM_TYPE = 0;

    private final TrackItemRenderer trackItemRenderer;

    @Inject
    public TracksRecyclerItemAdapter(TrackItemRenderer trackItemRenderer) {
        super(trackItemRenderer);
        this.trackItemRenderer = trackItemRenderer;
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
            if (item.isPlaying() || isCurrent) {
                item.setIsPlaying(isCurrent);
                notifyItemChanged(position);
            }
        }
    }

    @Override
    public void updateInRepeatMode(boolean isInRepeatMode) {
        for (int position = 0; position < getItemCount(); position++) {
            TrackItem item = getItem(position);
            if (item.isInRepeatMode() != isInRepeatMode) {
                item.setInRepeatMode(isInRepeatMode);
                notifyItemChanged(position);
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

    public void setTrackItemListener(TrackItemRenderer.Listener listener) {
        trackItemRenderer.setListener(listener);
    }

}
