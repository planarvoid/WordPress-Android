package com.soundcloud.android.tracks;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.propeller.PropertySet;

import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.List;

public class SyncableTrackItemPresenter extends TrackItemPresenter {

    @Inject
    public SyncableTrackItemPresenter(ImageOperations imageOperations, FeatureFlags featureFlags, TrackItemMenuController trackItemMenuController) {
        super(imageOperations, featureFlags, trackItemMenuController);
    }


    @Override
    public void bindItemView(int position, View itemView, List<PropertySet> trackItems) {
        super.bindItemView(position, itemView, trackItems);
        final PropertySet track = trackItems.get(position);

        final View downloadProgressIcon = itemView.findViewById(R.id.download_progress_icon);
        if (track.contains(TrackProperty.OFFLINE_DOWNLOADED_AT) && !track.contains(TrackProperty.OFFLINE_REMOVED_AT)){
            ((ImageView) downloadProgressIcon).setImageResource(R.drawable.track_downloaded);
            downloadProgressIcon.setVisibility(View.VISIBLE);
        } else {
            downloadProgressIcon.setVisibility(View.GONE);
        }
    }
}
