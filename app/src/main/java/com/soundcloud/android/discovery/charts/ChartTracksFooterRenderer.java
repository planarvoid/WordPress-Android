package com.soundcloud.android.discovery.charts;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class ChartTracksFooterRenderer implements CellRenderer<ChartTrackListItem> {

    private final Resources resources;

    @Inject
    public ChartTracksFooterRenderer(Resources resources) {
        this.resources = resources;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.chart_tracks_footer_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<ChartTrackListItem> items) {
        ChartTrackListItem.Footer item = (ChartTrackListItem.Footer) items.get(position);
        final String formattedTime = ScTextUtils.formatTimeElapsedSince(resources, item.lastUpdatedAt.getTime(), true);
        TextView textView = ButterKnife.findById(itemView, R.id.charts_footer);
        textView.setText(resources.getString(R.string.updated_time, formattedTime));
    }
}
