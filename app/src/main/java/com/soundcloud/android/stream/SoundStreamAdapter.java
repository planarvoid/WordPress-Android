package com.soundcloud.android.stream;

import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.view.adapters.PlaylistItemRenderer;

import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

public class SoundStreamAdapter extends PagingRecyclerItemAdapter<StreamItem, SoundStreamAdapter.SoundStreamViewHolder> {

    @VisibleForTesting static final int TRACK_ITEM_TYPE = 0;
    @VisibleForTesting static final int PLAYLIST_ITEM_TYPE = 1;

    private final TrackItemRenderer trackRenderer;

    @Inject
    public SoundStreamAdapter(TrackItemRenderer trackRenderer, PlaylistItemRenderer playlistRenderer) {
        super(new CellRendererBinding<>(TRACK_ITEM_TYPE, trackRenderer),
                new CellRendererBinding<>(PLAYLIST_ITEM_TYPE, playlistRenderer));
        this.trackRenderer = trackRenderer;
    }

    @Override
    public int getBasicItemViewType(int position) {
        if (getItem(position).getEntityUrn().isTrack()) {
            return TRACK_ITEM_TYPE;
        } else {
            return PLAYLIST_ITEM_TYPE;
        }
    }

    public TrackItemRenderer getTrackRenderer() {
        return trackRenderer;
    }

    @Override
    protected SoundStreamViewHolder createViewHolder(View itemView) {
        return new SoundStreamViewHolder(itemView);
    }

    public static class SoundStreamViewHolder extends RecyclerView.ViewHolder {
        public SoundStreamViewHolder(View itemView) {
            super(itemView);
        }
    }

}
