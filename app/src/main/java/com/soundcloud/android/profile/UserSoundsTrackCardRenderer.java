package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.UserSoundsItem.getPositionInModule;
import static com.soundcloud.android.profile.UserSoundsTypes.fromModule;

import com.soundcloud.android.R;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.TrackCardRenderer;
import com.soundcloud.java.optional.Optional;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class UserSoundsTrackCardRenderer extends UserSoundsItemRenderer {
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
        final UserSoundsItem userSoundsItem = items.get(position);
        final Optional<TrackItem> trackItem = userSoundsItem.trackItem();

        if (trackItem.isPresent()) {
            final TrackItem track = trackItem.get();
            itemView.setBackgroundColor(itemView.getResources().getColor(R.color.white));
            trackCardRenderer.bindTrackCard(track,
                                            itemView,
                                            Optional.of(fromModule(userSoundsItem.collectionType(), getPositionInModule(items, userSoundsItem))),
                                            createItemMenuOptions(userSoundsItem, track));
        }
    }

}
