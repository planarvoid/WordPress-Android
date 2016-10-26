package com.soundcloud.android.view.adapters;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.tracks.DownloadableTrackItemRenderer;
import com.soundcloud.android.tracks.TrackItem;

import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

public class PagedTracksRecyclerItemAdapter
        extends PagingRecyclerItemAdapter<TrackItem, RecyclerView.ViewHolder>
        implements PlayingTrackAware {

    @VisibleForTesting static final int TRACK_ITEM_TYPE = 0;

    @Inject
    public PagedTracksRecyclerItemAdapter(DownloadableTrackItemRenderer trackRenderer) {
        super(trackRenderer);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return TRACK_ITEM_TYPE;
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingUrn) {
        for (TrackItem item : getItems()) {
            item.setIsPlaying(item.getUrn().equals(currentlyPlayingUrn));
        }
        notifyDataSetChanged();
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }
}
