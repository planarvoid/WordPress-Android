package com.soundcloud.android.profile;

import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.java.optional.Optional;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class UserSoundsTrackItemRenderer implements CellRenderer<UserSoundsItem> {
    private final TrackItemRenderer trackItemRenderer;

    @Inject
    public UserSoundsTrackItemRenderer(TrackItemRenderer trackItemRenderer) {
        this.trackItemRenderer = trackItemRenderer;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return trackItemRenderer.createItemView(parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserSoundsItem> items) {
        final Optional<TrackItem> trackItem = items.get(position).getTrackItem();

        if (trackItem.isPresent()) {
            trackItemRenderer.bindTrackView(trackItem.get(), itemView, position);
        }
    }
}
