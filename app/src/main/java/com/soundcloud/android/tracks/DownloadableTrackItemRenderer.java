package com.soundcloud.android.tracks;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayPresenter;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.offline.DownloadImageView;
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Lazy;

import android.view.View;

import javax.inject.Inject;

public class DownloadableTrackItemRenderer extends TrackItemRenderer {

    @Inject
    public DownloadableTrackItemRenderer(ImageOperations imageOperations,
                                         CondensedNumberFormatter numberFormatter,
                                         TrackItemMenuPresenter trackItemMenuPresenter,
                                         EventBus eventBus,
                                         FeatureOperations featureOperations,
                                         ScreenProvider screenProvider,
                                         NavigationExecutor navigationExecutor,
                                         TrackItemView.Factory trackItemViewFactory,
                                         FeatureFlags featureFlags,
                                         OfflineSettingsOperations offlineSettingsOperations,
                                         NetworkConnectionHelper connectionHelper,
                                         Lazy<IntroductoryOverlayPresenter> introductoryOverlayPresenter) {
        super(imageOperations,
              numberFormatter,
              trackItemMenuPresenter,
              eventBus,
              screenProvider,
              navigationExecutor,
              featureOperations,
              trackItemViewFactory,
              featureFlags,
              offlineSettingsOperations,
              connectionHelper,
              introductoryOverlayPresenter);
    }

    @Override
    public void bindOfflineTrackView(TrackItem track,
                              View itemView,
                              int position,
                              Optional<TrackSourceInfo> trackSourceInfo,
                              Optional<Module> module) {
        super.bindOfflineTrackView(track, itemView, position, trackSourceInfo, module);
        setDownloadProgressIndicator(itemView, track);
    }

    private void setDownloadProgressIndicator(View itemView, TrackItem track) {
        final DownloadImageView downloadProgressIcon = (DownloadImageView) itemView.findViewById(R.id.item_download_state);
        if (featureFlags.isEnabled(Flag.NEW_OFFLINE_ICONS)) {
            downloadProgressIcon.setVisibility(View.GONE);
        } else {
            downloadProgressIcon.setState(featureOperations.isOfflineContentEnabled()
                                          ? track.offlineState()
                                          : OfflineState.NOT_OFFLINE, false);
        }
    }
}
