package com.soundcloud.android.tracks;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.utils.AnimUtils;

import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.List;

public class DownloadableTrackItemPresenter extends TrackItemPresenter {
    private final FeatureOperations featureOperations;

    @Inject
    public DownloadableTrackItemPresenter(ImageOperations imageOperations,
                                          TrackItemMenuPresenter trackItemMenuPresenter,
                                          FeatureOperations featureOperations) {
        super(imageOperations, trackItemMenuPresenter);
        this.featureOperations = featureOperations;
    }

    @Override
    public void bindItemView(int position, View itemView, List<TrackItem> trackItems) {
        super.bindItemView(position, itemView, trackItems);
        final TrackItem track = trackItems.get(position);

        setDownloadProgressIndicator(itemView, track);
    }

    private void setDownloadProgressIndicator(View itemView, TrackItem track) {

        final ImageView downloadProgressIcon = (ImageView) itemView.findViewById(R.id.download_progress_icon);
        downloadProgressIcon.clearAnimation();

        if (featureOperations.isOfflineContentEnabled()) {
            switch (track.getDownloadedState()) {
                case NO_OFFLINE:
                    setNoOfflineState(downloadProgressIcon);
                    break;
                case REQUESTED:
                    setPendingDownloadState(downloadProgressIcon);
                    break;
                case DOWNLOADING:
                    setDownloadingState(itemView, downloadProgressIcon);
                    break;
                case DOWNLOADED:
                    setDownloadedState(downloadProgressIcon);
                    break;
                case UNAVAILABLE:
                    setNoOfflineState(downloadProgressIcon);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown state: " + track.getDownloadedState());
            }
        } else {
            setNoOfflineState(downloadProgressIcon);
        }
    }

    private void setNoOfflineState(ImageView downloadProgressIcon) {
        downloadProgressIcon.setVisibility(View.GONE);
    }

    private void setPendingDownloadState(ImageView downloadProgressIcon) {
        downloadProgressIcon.setImageResource(R.drawable.entity_downloadable);
        downloadProgressIcon.setVisibility(View.VISIBLE);
    }

    private void setDownloadingState(View itemView, ImageView downloadProgressIcon) {
        downloadProgressIcon.setImageResource(R.drawable.entity_downloading);
        downloadProgressIcon.setVisibility(View.VISIBLE);
        AnimUtils.runSpinClockwiseAnimationOn(itemView.getContext(), downloadProgressIcon);
    }

    private void setDownloadedState(ImageView downloadProgressIcon) {
        downloadProgressIcon.setImageResource(R.drawable.entity_downloaded);
        downloadProgressIcon.setVisibility(View.VISIBLE);
    }
}
