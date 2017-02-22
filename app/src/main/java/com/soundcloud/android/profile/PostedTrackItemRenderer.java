package com.soundcloud.android.profile;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.tracks.TrackItemView;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import android.view.View;

import javax.inject.Inject;
import java.util.List;

class PostedTrackItemRenderer extends TrackItemRenderer {

    @Inject
    public PostedTrackItemRenderer(ImageOperations imageOperations,
                                   CondensedNumberFormatter numberFormatter,
                                   TrackItemMenuPresenter trackItemMenuPresenter,
                                   EventBus eventBus, ScreenProvider screenProvider,
                                   Navigator navigator, FeatureOperations featureOperations,
                                   TrackItemView.Factory trackItemViewFactory,
                                   FeatureFlags flags) {
        super(imageOperations, numberFormatter, trackItemMenuPresenter, eventBus,
              screenProvider, navigator, featureOperations, trackItemViewFactory, flags);
    }

    @Override
    public void bindItemView(int position, View itemView, List<TrackItem> trackItems) {
        super.bindItemView(position, itemView, trackItems);

        final TrackItem track = trackItems.get(position);
        final TrackItemView trackItemView = (TrackItemView) itemView.getTag();
        toggleReposterView(trackItemView, track);
    }

    private void toggleReposterView(TrackItemView itemView, TrackItem track) {
        final Optional<String> optionalReposter = track.reposter();
        if (optionalReposter.isPresent()) {
            itemView.showReposter(optionalReposter.get());
        } else {
            itemView.hideReposter();
        }
    }
}
