package com.soundcloud.android.tracks;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.crop.util.VisibleForTesting;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.propeller.PropertySet;

import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

public class DownloadableTrackItemPresenter extends TrackItemPresenter {
    private static final Date MIN_DATE = new Date(0L);
    private final FeatureOperations featureOperations;

    @Inject
    public DownloadableTrackItemPresenter(ImageOperations imageOperations,
                                          TrackItemMenuPresenter trackItemMenuPresenter,
                                          FeatureOperations featureOperations) {
        super(imageOperations, trackItemMenuPresenter);
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
        downloadProgressIcon.clearAnimation();

        if (featureOperations.isOfflineContentEnabled()) {
            if (isDownloaded(track)) {
                setDownloadedState(downloadProgressIcon);
            } else if (isPendingDownload(track)) {
                setPendingDownloadState(downloadProgressIcon);
            } else if (isDownloading(track)) {
                setDownloadingState(itemView, downloadProgressIcon);
            } else {
                setNoOfflineState(downloadProgressIcon);
            }
        } else {
            setNoOfflineState(downloadProgressIcon);
        }
    }

    @VisibleForTesting
    Boolean isDownloading(PropertySet track) {
        return track.getOrElse(TrackProperty.OFFLINE_DOWNLOADING, false);
    }

    @VisibleForTesting
    boolean isPendingDownload(PropertySet track) {
        final Date removedAt = track.getOrElse(TrackProperty.OFFLINE_REMOVED_AT, MIN_DATE);
        final Date unavailableAt = track.getOrElse(TrackProperty.OFFLINE_UNAVAILABLE_AT, MIN_DATE);
        return track.contains(TrackProperty.OFFLINE_REQUESTED_AT)
                && !track.getOrElse(TrackProperty.OFFLINE_DOWNLOADING, false)
                && track.get(TrackProperty.OFFLINE_REQUESTED_AT).after(removedAt)
                && track.get(TrackProperty.OFFLINE_REQUESTED_AT).after(unavailableAt);
    }

    @VisibleForTesting
    boolean isDownloaded(PropertySet track) {
        final Date removedAt = track.getOrElse(TrackProperty.OFFLINE_REMOVED_AT, MIN_DATE);
        return track.contains(TrackProperty.OFFLINE_DOWNLOADED_AT)
                && track.get(TrackProperty.OFFLINE_DOWNLOADED_AT).after(removedAt);
    }

    private void setNoOfflineState(ImageView downloadProgressIcon) {
        downloadProgressIcon.setVisibility(View.GONE);
    }

    private void setPendingDownloadState(ImageView downloadProgressIcon) {
        downloadProgressIcon.setImageResource(R.drawable.track_downloadable);
        downloadProgressIcon.setVisibility(View.VISIBLE);
    }

    private void setDownloadingState(View itemView, ImageView downloadProgressIcon) {
        downloadProgressIcon.setImageResource(R.drawable.track_downloading);
        downloadProgressIcon.setVisibility(View.VISIBLE);
        AnimUtils.runSpinClockwiseAnimationOn(itemView.getContext(), downloadProgressIcon);
    }

    private void setDownloadedState(ImageView downloadProgressIcon) {
        downloadProgressIcon.setImageResource(R.drawable.track_downloaded);
        downloadProgressIcon.setVisibility(View.VISIBLE);
    }
}
