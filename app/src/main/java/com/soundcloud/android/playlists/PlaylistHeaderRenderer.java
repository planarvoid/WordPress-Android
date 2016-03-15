package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

class PlaylistHeaderRenderer implements CellRenderer<PlaylistHeaderItem> {

    private final PlaylistHeaderPresenter playlistHeaderPresenter;

    public PlaylistHeaderRenderer(PlaylistHeaderPresenter playlistHeaderPresenter) {
        this.playlistHeaderPresenter = playlistHeaderPresenter;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_details_view, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, final List<PlaylistHeaderItem> items) {
        playlistHeaderPresenter.setPlaylist(items.get(position));
        playlistHeaderPresenter.setView(itemView);
    }
}
