package com.soundcloud.android.discovery.charts;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class ChartTracksHeaderRenderer implements CellRenderer<ChartTrackListItem> {

    @Inject
    public ChartTracksHeaderRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.chart_tracks_header_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<ChartTrackListItem> items) {
        ChartTrackListItem.Header item = (ChartTrackListItem.Header) items.get(position);
        TextView textView = ButterKnife.findById(itemView, R.id.charts_header);
        textView.setText(item.type == ChartType.TOP ? R.string.charts_top_fifty_list_header : R.string.charts_new_and_hot_list_header);
    }
}
