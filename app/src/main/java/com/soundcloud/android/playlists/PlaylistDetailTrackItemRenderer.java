package com.soundcloud.android.playlists;

import com.google.auto.factory.AutoFactory;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.PlaylistTrackItemRenderer;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;

@AutoFactory
public class PlaylistDetailTrackItemRenderer implements CellRenderer<PlaylistDetailTrackItem> {

    private final PlaylistTrackItemRenderer playlistTrackItemRenderer;

    public PlaylistDetailTrackItemRenderer(PlaylistTrackItemRenderer playlistTrackItemRenderer) {
        this.playlistTrackItemRenderer = playlistTrackItemRenderer;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return playlistTrackItemRenderer.createItemView(parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PlaylistDetailTrackItem> items) {
        playlistTrackItemRenderer.bindTrackView(position,
                                                itemView,
                                                items.get(position).getTrackItem());
    }

}
