package com.soundcloud.android.playlists;

import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.presentation.RecyclerItemAdapter;

import android.view.View;

import javax.inject.Inject;

public class PlaylistLikesAdapter extends PagingRecyclerItemAdapter<PlaylistItem, RecyclerItemAdapter.ViewHolder> {

    private static final int PLAYLIST_ITEM_VIEW_TYPE = 0;

    @Inject
    public PlaylistLikesAdapter(DownloadablePlaylistItemRenderer playlistRenderer) {
        super(playlistRenderer);
    }

    @Override
    protected RecyclerItemAdapter.ViewHolder createViewHolder(View view) {
        return new RecyclerItemAdapter.ViewHolder(view);
    }

    @Override
    public int getBasicItemViewType(int i) {
        return PLAYLIST_ITEM_VIEW_TYPE;
    }
}
