package com.soundcloud.android.stream;

import com.soundcloud.android.R;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

public class StreamPlaylistItemRenderer implements CellRenderer<PlaylistItem> {

    @Inject
    public StreamPlaylistItemRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.stream_playlist_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PlaylistItem> items) {

    }
}
