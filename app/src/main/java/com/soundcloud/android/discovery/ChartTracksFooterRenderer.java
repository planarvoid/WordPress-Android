package com.soundcloud.android.discovery;

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

class ChartTracksFooterRenderer implements CellRenderer<ChartTrackItem> {

    private final Resources resources;

    @Inject
    public ChartTracksFooterRenderer(Resources resources) {
        this.resources = resources;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.chart_tracks_header_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<ChartTrackItem> items) {
        ChartTrackItem.Footer item = (ChartTrackItem.Footer) items.get(position);
        final String formattedTime = ScTextUtils.formatTimeElapsedSince(resources, item.lastUpdatedAt, true);
        ((TextView) itemView).setText(resources.getString(R.string.last_updated, formattedTime));
    }
}
