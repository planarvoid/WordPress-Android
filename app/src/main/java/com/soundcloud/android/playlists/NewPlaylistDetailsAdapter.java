package com.soundcloud.android.playlists;

import com.google.auto.factory.AutoFactory;
import com.soundcloud.android.presentation.RecyclerItemAdapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;

@AutoFactory
class NewPlaylistDetailsAdapter extends RecyclerItemAdapter<PlaylistDetailTrackItem, RecyclerView.ViewHolder> {

    NewPlaylistDetailsAdapter(PlaylistDetailTrackItemRenderer playlistTrackItemRenderer) {
        super(playlistTrackItemRenderer);
    }

    @Override
    protected RecyclerView.ViewHolder createViewHolder(View itemView) {
        return new RecyclerView.ViewHolder(itemView) {
        };
    }

    @Override
    public int getBasicItemViewType(int position) {
        return 0;
    }
}
