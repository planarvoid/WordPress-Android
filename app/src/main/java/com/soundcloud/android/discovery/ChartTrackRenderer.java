package com.soundcloud.android.discovery;

import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class ChartTrackRenderer implements CellRenderer<ChartTrackListItem> {

    private final TrackItemRenderer trackItemRenderer;

    @Inject
    ChartTrackRenderer(TrackItemRenderer trackItemRenderer) {
        this.trackItemRenderer = trackItemRenderer;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return trackItemRenderer.createItemView(parent);
    }

    @Override
    public void bindItemView(final int position, View itemView, List<ChartTrackListItem> items) {
        final List<TrackItem> trackItems = Lists.transform(items, toPositionedChartTrackItem(position));
        trackItemRenderer.bindItemView(position, itemView, trackItems);
    }

    private static Function<ChartTrackListItem, TrackItem> toPositionedChartTrackItem(final int position) {
        return new Function<ChartTrackListItem, TrackItem>() {
            public TrackItem apply(ChartTrackListItem input) {
                return ((ChartTrackListItem.Track) input).chartTrackItem.copyWithPosition(position);
            }
        };
    }
}
