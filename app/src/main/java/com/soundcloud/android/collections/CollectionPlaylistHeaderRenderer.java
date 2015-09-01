package com.soundcloud.android.collections;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class CollectionPlaylistHeaderRenderer implements CellRenderer<CollectionsItem> {

    @Inject
    public CollectionPlaylistHeaderRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.collection_playlist_header, parent, false);
    }

    @Override
    public void bindItemView(int position, View view, List<CollectionsItem> list) {
        // no-op
    }
}
