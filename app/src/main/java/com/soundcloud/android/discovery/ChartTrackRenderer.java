package com.soundcloud.android.discovery;

import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import org.jetbrains.annotations.Nullable;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class ChartTrackRenderer implements CellRenderer<ChartTrackItem> {

    private final TrackItemRenderer trackItemRenderer;
    private final static Function<ChartTrackItem, TrackItem> CHART_TRACK_TO_TRACK_ITEM =
            new Function<ChartTrackItem, TrackItem>() {
        @Nullable
        @Override
        public TrackItem apply(ChartTrackItem input) {
            return ((ChartTrackItem.Track) input).trackItem;
        }
    };

    @Inject
    ChartTrackRenderer(TrackItemRenderer trackItemRenderer) {
        this.trackItemRenderer = trackItemRenderer;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return trackItemRenderer.createItemView(parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<ChartTrackItem> items) {
        final List<TrackItem> trackItems = Lists.transform(items, CHART_TRACK_TO_TRACK_ITEM);
        trackItemRenderer.bindItemView(position, itemView, trackItems);
    }
}
