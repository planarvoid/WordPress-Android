package com.soundcloud.android.playlists;

import com.soundcloud.android.collections.ItemAdapter;
import com.soundcloud.android.collections.views.PlayableRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.observers.EmptyViewAware;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.android.view.EmptyListViewFactory;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

class InlinePlaylistTracksAdapter extends ItemAdapter<Track> implements EmptyViewAware {

    private static final int INITIAL_SIZE = 20;

    private final ImageOperations mImageOperations;

    private int mEmptyViewStatus = EmptyListView.Status.WAITING;

    InlinePlaylistTracksAdapter(ImageOperations imageOperations) {
        super(INITIAL_SIZE);
        mImageOperations = imageOperations;
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.isEmpty() ? IGNORE_ITEM_VIEW_TYPE : super.getItemViewType(position);
    }

    @Override
    protected View createItemView(int position, ViewGroup parent) {
        Context context = parent.getContext();
        if (getItemViewType(position) == IGNORE_ITEM_VIEW_TYPE) {
            return new EmptyListViewFactory().build(context);
        } else {
            return new PlayableRow(context, mImageOperations);
        }
    }

    @Override
    protected void bindItemView(int position, View itemView) {
        if (getItemViewType(position) == IGNORE_ITEM_VIEW_TYPE) {
            ((EmptyListView) itemView).setStatus(mEmptyViewStatus);
        } else {
            ((PlayableRow) itemView).display(position, mItems.get(position));
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getCount() {
        return Math.max(1, mItems.size()); // at least 1 for the empty view
    }

    public void setEmptyViewStatus(int emptyViewStatus){
        mEmptyViewStatus = emptyViewStatus;
        notifyDataSetChanged();
    }
}
