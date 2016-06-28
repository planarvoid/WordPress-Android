package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class ChartTracksHeaderRenderer implements CellRenderer<ChartTrackItem> {

    @Inject
    public ChartTracksHeaderRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.chart_tracks_header_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<ChartTrackItem> items) {
        ChartTrackItem.Header item = (ChartTrackItem.Header) items.get(position);
        ((TextView) itemView).setText(item.type == ChartType.TOP ? R.string.charts_top_fifty_list_header : R.string.charts_new_and_hot_list_header);
    }
}
