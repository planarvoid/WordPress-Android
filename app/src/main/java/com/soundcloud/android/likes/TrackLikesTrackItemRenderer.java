package com.soundcloud.android.likes;

import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.java.optional.Optional;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class TrackLikesTrackItemRenderer implements CellRenderer<TrackLikesTrackItem> {

    private final TrackItemRenderer trackItemRenderer;

    @Inject
    TrackLikesTrackItemRenderer(TrackItemRenderer trackItemRenderer) {
        this.trackItemRenderer = trackItemRenderer;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return trackItemRenderer.createItemView(parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<TrackLikesTrackItem> items) {
        trackItemRenderer.bindOfflineTrackView(items.get(position).getTrackItem(),
                                        itemView,
                                        position,
                                        Optional.absent(),
                                        Optional.absent());
    }
}
