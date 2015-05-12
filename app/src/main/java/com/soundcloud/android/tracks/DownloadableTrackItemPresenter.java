package com.soundcloud.android.tracks;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.offline.DownloadImageView;
import com.soundcloud.android.offline.DownloadState;
import com.soundcloud.android.rx.eventbus.EventBus;

import android.view.View;

import javax.inject.Inject;
import java.util.List;

public class DownloadableTrackItemPresenter extends TrackItemPresenter {

    private final FeatureOperations featureOperations;

    @Inject
    public DownloadableTrackItemPresenter(ImageOperations imageOperations,
                                          TrackItemMenuPresenter trackItemMenuPresenter,
                                          EventBus eventBus,
                                          FeatureOperations featureOperations,
                                          ScreenProvider screenProvider) {
        super(imageOperations, trackItemMenuPresenter, eventBus, screenProvider);
        this.featureOperations = featureOperations;
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
            downloadProgressIcon.setState(DownloadState.NO_OFFLINE);
        }
    }
}
