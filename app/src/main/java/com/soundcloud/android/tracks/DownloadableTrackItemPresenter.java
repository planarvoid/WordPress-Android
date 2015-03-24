package com.soundcloud.android.tracks;

import com.google.common.base.Optional;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.crop.util.VisibleForTesting;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.utils.AnimUtils;

import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.Date;
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
    Boolean isDownloading(TrackItem track) {
        return track.isDownloading();
    }

    @VisibleForTesting
    boolean isPendingDownload(TrackItem track) {
        final Date removedAt = track.getDownloadRemovedAt();
        final Date unavailableAt = track.getDownloadUnavailableAt();
        final Optional<Date> downloadRequestedAt = track.getDownloadRequestedAt();
        return !track.isDownloading()
                && downloadRequestedAt.isPresent()
                && downloadRequestedAt.get().after(removedAt)
                && downloadRequestedAt.get().after(unavailableAt);
    }

    @VisibleForTesting
    boolean isDownloaded(TrackItem track) {
        final Date removedAt = track.getDownloadRemovedAt();
        final Optional<Date> maybeDownloadedAt = track.getDownloadedAt();
        return maybeDownloadedAt.isPresent() && maybeDownloadedAt.get().after(removedAt);
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
