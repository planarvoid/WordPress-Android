package com.soundcloud.android.tracks;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.offline.DownloadImageView;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

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
                                         Navigator navigator,
                                         TrackItemView.Factory trackItemViewFactory) {
        super(imageOperations, numberFormatter, trackItemMenuPresenter,
              eventBus, screenProvider, navigator,
              featureOperations, trackItemViewFactory);
    }

    protected DownloadableTrackItemRenderer(Optional<String> moduleName,
                                            ImageOperations imageOperations,
                                            CondensedNumberFormatter numberFormatter,
                                            TrackItemMenuPresenter trackItemMenuPresenter,
                                            EventBus eventBus,
                                            FeatureOperations featureOperations,
                                            ScreenProvider screenProvider,
                                            Navigator navigator,
                                            TrackItemView.Factory trackItemViewFactory) {
        super(moduleName,
              imageOperations,
              numberFormatter,
              trackItemMenuPresenter,
              eventBus,
              screenProvider,
              navigator,
              featureOperations,
              trackItemViewFactory);
    }

    @Override
    public void bindTrackView(TrackItem track,
                              View itemView,
                              int position,
                              Optional<TrackSourceInfo> trackSourceInfo,
                              Optional<Module> module) {
        super.bindTrackView(track, itemView, position, trackSourceInfo, module);
        setDownloadProgressIndicator(itemView, track);
    }

    private void setDownloadProgressIndicator(View itemView, TrackItem track) {
        final DownloadImageView downloadProgressIcon = (DownloadImageView) itemView.findViewById(R.id.item_download_state);
        if (featureOperations.isOfflineContentEnabled()) {
            downloadProgressIcon.setState(track.offlineState());
        } else {
            downloadProgressIcon.setState(OfflineState.NOT_OFFLINE);
        }
    }
}
