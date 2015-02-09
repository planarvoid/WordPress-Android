package com.soundcloud.android.tracks;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.propeller.PropertySet;

import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.List;

public class SyncableTrackItemPresenter extends TrackItemPresenter {
    private final FeatureOperations featureOperations;

    @Inject
    public SyncableTrackItemPresenter(ImageOperations imageOperations, FeatureFlags featureFlags,
                                      TrackItemMenuController trackItemMenuController,
                                      FeatureOperations featureOperations) {
        super(imageOperations, featureFlags, trackItemMenuController);
        this.featureOperations = featureOperations;
    }


    @Override
    public void bindItemView(int position, View itemView, List<PropertySet> trackItems) {
        super.bindItemView(position, itemView, trackItems);
        final PropertySet track = trackItems.get(position);

        setDownloadProgressIndicator(itemView, track);
    }

    private void setDownloadProgressIndicator(View itemView, PropertySet track) {

        final ImageView downloadProgressIcon = (ImageView) itemView.findViewById(R.id.download_progress_icon);

        if (featureOperations.isOfflineContentEnabled()){
            if (track.contains(TrackProperty.OFFLINE_DOWNLOADED_AT) && !track.contains(TrackProperty.OFFLINE_REMOVED_AT)){
                downloadProgressIcon.setImageResource(R.drawable.track_downloaded);
                downloadProgressIcon.setVisibility(View.VISIBLE);

            } else if (track.contains(TrackProperty.OFFLINE_REQUESTED_AT) && !track.contains(TrackProperty.OFFLINE_REMOVED_AT)) {
                downloadProgressIcon.setImageResource(R.drawable.track_downloading);
                downloadProgressIcon.setVisibility(View.VISIBLE);

            } else {
                downloadProgressIcon.setVisibility(View.GONE);
            }
        }
    }
}
