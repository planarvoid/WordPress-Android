package com.soundcloud.android.collection.recentlyplayed;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class RecentlyPlayedEmptyRenderer implements CellRenderer<RecentlyPlayedEmpty> {

    @Inject
    RecentlyPlayedEmptyRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.recently_played_empty, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<RecentlyPlayedEmpty> items) {
        itemView.setEnabled(false);
    }
}
