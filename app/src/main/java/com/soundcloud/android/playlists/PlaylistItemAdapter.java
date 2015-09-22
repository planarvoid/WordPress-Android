package com.soundcloud.android.playlists;

import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

public class PlaylistItemAdapter extends PagingRecyclerItemAdapter<PlaylistItem, RecyclerView.ViewHolder> {

    private static final int PLAYLIST_ITEM_VIEW_TYPE = 0;

    @Inject
    public PlaylistItemAdapter(DownloadablePlaylistItemRenderer playlistRenderer) {
        super(playlistRenderer);
    }

    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new PagingRecyclerItemAdapter.ViewHolder(view);
    }

    @Override
    public int getBasicItemViewType(int i) {
        return PLAYLIST_ITEM_VIEW_TYPE;
    }
}
