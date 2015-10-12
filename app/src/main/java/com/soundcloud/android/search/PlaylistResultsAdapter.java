package com.soundcloud.android.search;

import com.soundcloud.android.R;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.presentation.ProgressCellRenderer;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.view.adapters.PlaylistGridRenderer;

import android.view.View;

import javax.inject.Inject;

class PlaylistResultsAdapter extends PagingRecyclerItemAdapter<PlaylistItem, RecyclerItemAdapter.ViewHolder> {

    @Inject
    PlaylistResultsAdapter(PlaylistGridRenderer cellRenderer) {
        super(cellRenderer, new ProgressCellRenderer(R.layout.grid_loading_item));
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return 0;
    }
}
