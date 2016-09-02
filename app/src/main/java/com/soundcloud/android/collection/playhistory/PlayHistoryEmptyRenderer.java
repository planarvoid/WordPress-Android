package com.soundcloud.android.collection.playhistory;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class PlayHistoryEmptyRenderer implements CellRenderer<PlayHistoryItemEmpty> {

    @Inject
    PlayHistoryEmptyRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.play_history_empty, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PlayHistoryItemEmpty> items) {
        itemView.setEnabled(false);
    }
}
