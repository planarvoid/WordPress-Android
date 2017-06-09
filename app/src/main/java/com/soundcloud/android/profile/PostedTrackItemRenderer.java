package com.soundcloud.android.profile;

import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.GoOnboardingTooltipExperiment;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayPresenter;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.tracks.TrackItemView;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Lazy;

import android.view.View;

import javax.inject.Inject;
import java.util.List;

class PostedTrackItemRenderer extends TrackItemRenderer {

    @Inject
    PostedTrackItemRenderer(ImageOperations imageOperations,
                            CondensedNumberFormatter numberFormatter,
                            TrackItemMenuPresenter trackItemMenuPresenter,
                            EventBus eventBus,
                            ScreenProvider screenProvider,
                            NavigationExecutor navigationExecutor,
                            FeatureOperations featureOperations,
                            TrackItemView.Factory trackItemViewFactory,
                            OfflineSettingsOperations offlineSettingsOperations,
                            NetworkConnectionHelper connectionHelper,
                            GoOnboardingTooltipExperiment goOnboardingTooltipExperiment,
                            Lazy<IntroductoryOverlayPresenter> introductoryOverlayPresenter) {
        super(imageOperations,
              numberFormatter,
              trackItemMenuPresenter,
              eventBus,
              screenProvider,
              navigationExecutor,
              featureOperations,
              trackItemViewFactory,
              offlineSettingsOperations,
              connectionHelper,
              goOnboardingTooltipExperiment,
              introductoryOverlayPresenter);
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
