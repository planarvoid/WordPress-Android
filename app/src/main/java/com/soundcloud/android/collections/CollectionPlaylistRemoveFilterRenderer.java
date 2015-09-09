package com.soundcloud.android.collections;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class CollectionPlaylistRemoveFilterRenderer implements CellRenderer<CollectionsItem> {

    private OnRemoveFilterListener onRemoveFilterClickListener;
    private final View.OnClickListener onRemoveFilterClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (onRemoveFilterClickListener != null) {
                onRemoveFilterClickListener.onRemoveFilter();
            }
        }
    };;

    interface OnRemoveFilterListener {
        void onRemoveFilter();
    }

    @Inject
    public CollectionPlaylistRemoveFilterRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.collection_kill_filters, parent, false);
        view.findViewById(R.id.btn_remove_filters).setOnClickListener(onRemoveFilterClicked);
        return view;
    }

    @Override
    public void bindItemView(int position, View view, List<CollectionsItem> list) {
        // no-op
    }

    public void setOnRemoveFilterClickListener(OnRemoveFilterListener onSettingsClickListener) {
        this.onRemoveFilterClickListener = onSettingsClickListener;
    }
}
