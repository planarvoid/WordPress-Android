package com.soundcloud.android.playlists;

import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

public class PlaylistLikesAdapter extends PagingRecyclerItemAdapter<PlaylistItem, PlaylistLikesAdapter.ViewHolder> {

    private static final int PLAYLIST_ITEM_VIEW_TYPE = 0;

    @Inject
    public PlaylistLikesAdapter(DownloadablePlaylistItemRenderer playlistRenderer) {
        super(playlistRenderer);
    }

    @Override
    protected ViewHolder createViewHolder(View view) {
        return new ViewHolder(view);
    }

    @Override
    public int getBasicItemViewType(int i) {
        return PLAYLIST_ITEM_VIEW_TYPE;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
