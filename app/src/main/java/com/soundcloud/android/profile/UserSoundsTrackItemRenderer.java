package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.UserSoundsItem.getPositionInModule;
import static com.soundcloud.android.profile.UserSoundsTypes.fromModule;

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
        return trackItemViewFactory.createItemView(parent, R.layout.track_list_item_with_divider);
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserSoundsItem> items) {
        final UserSoundsItem userSoundsItem = items.get(position);
        final Optional<TrackItem> trackItem = userSoundsItem.trackItem();

        if (trackItem.isPresent()) {
            itemView.setBackgroundColor(itemView.getResources().getColor(R.color.white));
            trackItemRenderer.bindTrackView(trackItem.get(),
                                            itemView,
                                            position,
                                            Optional.absent(),
                                            Optional.of(fromModule(userSoundsItem.collectionType(),
                                                                   getPositionInModule(items, userSoundsItem))));
        }
    }
}
