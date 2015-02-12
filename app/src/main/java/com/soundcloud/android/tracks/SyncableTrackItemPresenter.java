package com.soundcloud.android.tracks;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.propeller.PropertySet;

import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.List;

public class SyncableTrackItemPresenter extends TrackItemPresenter {
    private final FeatureOperations featureOperations;
    private final OfflineContentOperations offlineContentOperations;

    @Inject
    public SyncableTrackItemPresenter(ImageOperations imageOperations,
                                      TrackItemMenuController trackItemMenuController,
                                      FeatureOperations featureOperations,
                                      OfflineContentOperations offlineContentOperations) {
        super(imageOperations, trackItemMenuController);
        this.featureOperations = featureOperations;
        this.offlineContentOperations = offlineContentOperations;
    }

    @Override
    public void bindItemView(int position, View itemView, List<PropertySet> trackItems) {
        super.bindItemView(position, itemView, trackItems);
        final PropertySet track = trackItems.get(position);

        setDownloadProgressIndicator(itemView, track);
    }

    private void setDownloadProgressIndicator(View itemView, PropertySet track) {

        final ImageView downloadProgressIcon = (ImageView) itemView.findViewById(R.id.download_progress_icon);
        downloadProgressIcon.clearAnimation();

        if (featureOperations.isOfflineContentEnabled() && offlineContentOperations.isOfflineLikesEnabled()) {
            if (track.contains(TrackProperty.OFFLINE_DOWNLOADED_AT) && !track.contains(TrackProperty.OFFLINE_REMOVED_AT)) {
                downloadProgressIcon.setImageResource(R.drawable.track_downloaded);
                downloadProgressIcon.setVisibility(View.VISIBLE);

            } else if (track.getOrElse(TrackProperty.OFFLINE_DOWNLOADING, false)) {
                downloadProgressIcon.setImageResource(R.drawable.track_downloading);
                downloadProgressIcon.setVisibility(View.VISIBLE);

                AnimUtils.runSpinClockwiseAnimationOn(itemView.getContext(), downloadProgressIcon);
            } else if (track.contains(TrackProperty.OFFLINE_REQUESTED_AT) && !track.contains(TrackProperty.OFFLINE_REMOVED_AT)) {
                downloadProgressIcon.setImageResource(R.drawable.track_downloadable);
                downloadProgressIcon.setVisibility(View.VISIBLE);
            } else {
                downloadProgressIcon.setVisibility(View.GONE);
            }
        } else {
            downloadProgressIcon.setVisibility(View.GONE);
        }
    }
}
