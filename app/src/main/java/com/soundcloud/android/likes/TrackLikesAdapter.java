package com.soundcloud.android.likes;

import static com.soundcloud.android.likes.TrackLikesItem.Kind.TrackItem;

import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.presentation.RecyclerItemAdapter;

import android.view.View;

import javax.inject.Inject;

class TrackLikesAdapter extends PagingRecyclerItemAdapter<TrackLikesItem, RecyclerItemAdapter.ViewHolder> {

    @Inject
    public TrackLikesAdapter(TrackLikesTrackItemRenderer trackLikesTrackItemRenderer) {
        super(new CellRendererBinding<>(TrackItem.ordinal(), trackLikesTrackItemRenderer));
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).getKind().ordinal();
    }
}
