package com.soundcloud.android.collections;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class CollectionPlaylistHeaderRenderer implements CellRenderer<CollectionsItem> {

    private OnSettingsClickListener onSettingsClickListener;
    private final View.OnClickListener onSettingsClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (onSettingsClickListener != null) {
                onSettingsClickListener.onSettingsClicked(view);
            }
        }
    };;

    interface OnSettingsClickListener {
        void onSettingsClicked(View view);
    }

    @Inject
    public CollectionPlaylistHeaderRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.collection_playlist_header, parent, false);
        view.findViewById(R.id.btn_collections_playlist_options).setOnClickListener(onSettingsClicked);
        return view;
    }

    @Override
    public void bindItemView(int position, View view, List<CollectionsItem> list) {
        // no-op
    }

    public void setOnSettingsClickListener(OnSettingsClickListener onSettingsClickListener) {
        this.onSettingsClickListener = onSettingsClickListener;
    }
}
