package com.soundcloud.android.stations;

import com.soundcloud.android.presentation.CellRenderer;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class RecentStationRenderer implements CellRenderer<Station> {

    @Inject
    public RecentStationRenderer() {
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        return new TextView(viewGroup.getContext());
    }

    @Override
    public void bindItemView(int position, View view, List<Station> stations) {
        final Station station = stations.get(position);
        ((TextView) view).setText(station.getTitle());
    }
}
