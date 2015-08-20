package com.soundcloud.android.tracks;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.offline.DownloadImageView;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.rx.eventbus.EventBus;

import android.view.View;

import javax.inject.Inject;
import java.util.List;

public class DownloadableTrackItemRenderer extends TrackItemRenderer {

    @Inject
    public DownloadableTrackItemRenderer(ImageOperations imageOperations,
                                         TrackItemMenuPresenter trackItemMenuPresenter,
                                         EventBus eventBus,
                                         FeatureOperations featureOperations,
                                         ScreenProvider screenProvider,
                                         Navigator navigator,
                                         TrackItemView.Factory trackItemViewFactory) {
        super(imageOperations, trackItemMenuPresenter,
                eventBus, screenProvider, navigator,
                featureOperations, trackItemViewFactory);
    }

    @Override
    public void bindItemView(int position, View itemView, List<TrackItem> trackItems) {
        super.bindItemView(position, itemView, trackItems);
        final TrackItem track = trackItems.get(position);

        setDownloadProgressIndicator(itemView, track);
    }

    private void setDownloadProgressIndicator(View itemView, TrackItem track) {
        final DownloadImageView downloadProgressIcon = (DownloadImageView) itemView.findViewById(R.id.item_download_state);

        if (featureOperations.isOfflineContentEnabled()) {
            downloadProgressIcon.setState(track.getDownloadedState());
        } else {
            downloadProgressIcon.setState(OfflineState.NO_OFFLINE);
        }
    }
}
