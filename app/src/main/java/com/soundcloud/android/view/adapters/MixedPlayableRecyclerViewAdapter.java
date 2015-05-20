package com.soundcloud.android.view.adapters;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.tracks.TrackItemPresenter;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

public class MixedPlayableRecyclerViewAdapter extends PagingRecyclerViewAdapter<PlayableItem, MixedPlayableRecyclerViewAdapter.MixedPlayableViewHolder> {

    @VisibleForTesting static final int TRACK_ITEM_TYPE = 0;
    @VisibleForTesting static final int PLAYLIST_ITEM_TYPE = 1;

    private final TrackItemPresenter trackPresenter;

    @Inject
    public MixedPlayableRecyclerViewAdapter(TrackItemPresenter trackPresenter, PlaylistItemPresenter playlistPresenter) {
        super(new ProgressCellPresenter(R.layout.list_loading_item),
                new CellPresenterBinding<>(TRACK_ITEM_TYPE, trackPresenter),
                new CellPresenterBinding<>(PLAYLIST_ITEM_TYPE, playlistPresenter));
        this.trackPresenter = trackPresenter;
    }

    @Override
    public int getItemViewType(int position) {
        final int itemViewType = super.getItemViewType(position);
        if (itemViewType == PROGRESS_VIEW_TYPE) {
            return itemViewType;
        } else if (getItem(position).getEntityUrn().isTrack()) {
            return TRACK_ITEM_TYPE;
        } else {
            return PLAYLIST_ITEM_TYPE;
        }
    }

    public TrackItemPresenter getTrackPresenter() {
        return trackPresenter;
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
