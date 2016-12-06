package com.soundcloud.android.playlists;

import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.PlaylistTrackItemRenderer;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

public class PlaylistDetailTrackItemRenderer implements CellRenderer<PlaylistDetailTrackItem> {

    private final PlaylistTrackItemRenderer playlistTrackItemRenderer;

    @Inject
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
