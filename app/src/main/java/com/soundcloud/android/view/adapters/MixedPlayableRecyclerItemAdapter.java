package com.soundcloud.android.view.adapters;

import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.tracks.TrackItemRenderer;

import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

public class MixedPlayableRecyclerItemAdapter
        extends PagingRecyclerItemAdapter<PlayableItem, MixedPlayableRecyclerItemAdapter.MixedPlayableViewHolder> {

    @VisibleForTesting static final int TRACK_ITEM_TYPE = 0;
    @VisibleForTesting static final int PLAYLIST_ITEM_TYPE = 1;

    private final TrackItemRenderer trackRenderer;

    @Inject
    public MixedPlayableRecyclerItemAdapter(TrackItemRenderer trackRenderer, PlaylistItemRenderer playlistRenderer) {
        super(new CellRendererBinding<>(TRACK_ITEM_TYPE, trackRenderer),
              new CellRendererBinding<>(PLAYLIST_ITEM_TYPE, playlistRenderer));
        this.trackRenderer = trackRenderer;
    }

    @Override
    public int getBasicItemViewType(int position) {
        if (getItem(position).getUrn().isTrack()) {
            return TRACK_ITEM_TYPE;
        } else {
            return PLAYLIST_ITEM_TYPE;
        }
    }

    public TrackItemRenderer getTrackRenderer() {
        return trackRenderer;
    }

    @Override
    protected MixedPlayableViewHolder createViewHolder(View itemView) {
        return new MixedPlayableViewHolder(itemView);
    }

    public static class MixedPlayableViewHolder extends RecyclerView.ViewHolder {
        public MixedPlayableViewHolder(View itemView) {
            super(itemView);
        }
    }

}