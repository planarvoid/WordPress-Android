package com.soundcloud.android.collections;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class CollectionEmptyPlaylistsRenderer implements CellRenderer<CollectionsItem> {

    @Inject
    public CollectionEmptyPlaylistsRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.empty_collections_playlists_view, parent, false);
    }

    @Override
    public void bindItemView(int position, View view, List<CollectionsItem> list) {
        // no-op
    }
}
