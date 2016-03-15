package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.TrackCardRenderer;
import com.soundcloud.java.optional.Optional;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class UserSoundsTrackCardRenderer implements CellRenderer<UserSoundsItem> {
    private final TrackCardRenderer trackCardRenderer;

    @Inject
    public UserSoundsTrackCardRenderer(TrackCardRenderer trackCardRenderer) {
        this.trackCardRenderer = trackCardRenderer;
        trackCardRenderer.setLayoutResource(R.layout.profile_user_sounds_track_card);
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return trackCardRenderer.createItemView(parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserSoundsItem> items) {
        final Optional<TrackItem> trackItem = items.get(position).getTrackItem();

        if (trackItem.isPresent()) {
            trackCardRenderer.bindTrackCard(trackItem.get(), itemView, position);
        }
    }
}
