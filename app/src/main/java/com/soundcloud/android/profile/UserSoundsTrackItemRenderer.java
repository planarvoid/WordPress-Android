package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.tracks.TrackItemView;
import com.soundcloud.java.optional.Optional;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class UserSoundsTrackItemRenderer implements CellRenderer<UserSoundsItem> {
    private final TrackItemRenderer trackItemRenderer;
    private final TrackItemView.Factory trackItemViewFactory;

    @Inject
    public UserSoundsTrackItemRenderer(TrackItemRenderer trackItemRenderer,
                                       TrackItemView.Factory trackItemViewFactory) {
        this.trackItemRenderer = trackItemRenderer;
        this.trackItemViewFactory = trackItemViewFactory;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return trackItemViewFactory.createItemView(parent, R.layout.profile_user_sounds_track_list_item);
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserSoundsItem> items) {
        final Optional<TrackItem> trackItem = items.get(position).getTrackItem();

        if (trackItem.isPresent()) {
            trackItemRenderer.bindTrackView(trackItem.get(), itemView, position);
        }
    }
}
