package com.soundcloud.android.collection.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class EmptyPlaylistsRenderer implements CellRenderer<PlaylistCollectionEmptyPlaylistItem> {

    @Inject
    public EmptyPlaylistsRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext())
                             .inflate(R.layout.empty_collections_playlists_view, parent, false);
    }

    @Override
    public void bindItemView(int position, View view, List<PlaylistCollectionEmptyPlaylistItem> list) {
        // no-op
    }
}
