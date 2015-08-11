package com.soundcloud.android.discovery;

import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class RecommendedTrackItemRenderer implements CellRenderer<RecommendedTrackItem> {

    private final TrackItemRenderer trackItemRenderer;

    @Inject
    public RecommendedTrackItemRenderer(TrackItemRenderer trackItemRenderer) {
        this.trackItemRenderer = trackItemRenderer;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        return this.trackItemRenderer.createItemView(viewGroup);
    }

    @Override
    public void bindItemView(int position, View itemView, List<RecommendedTrackItem> recommendedTrackItems) {
        this.trackItemRenderer.bindItemView(position, itemView, new ArrayList<TrackItem>(recommendedTrackItems));
    }
}
