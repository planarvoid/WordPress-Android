package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.ui.progress.ScrubController.OnScrubListener;
import static com.soundcloud.android.playback.ui.progress.ScrubController.SCRUB_STATE_CANCELLED;
import static com.soundcloud.android.playback.ui.progress.ScrubController.SCRUB_STATE_SCRUBBING;
import static com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView.OnWidthChangedListener;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.playback.ui.progress.ProgressController;
import com.soundcloud.android.playback.ui.progress.TranslateXHelper;
import com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView;
import com.soundcloud.android.tracks.TrackUrn;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;

public class PlayerArtworkController implements ProgressAware, OnScrubListener, OnWidthChangedListener, ImageListener {
    private final PlayerTrackArtworkView artworkView;
    private final ImageView wrappedImageView;
    private final ProgressController progressController;
    private final ImageOperations imageOperations;

    private TranslateXHelper helper;
    private boolean suppressProgress;

    private PlaybackProgress latestProgress = PlaybackProgress.empty();
    private boolean isPlaying;

    public PlayerArtworkController(PlayerTrackArtworkView artworkView,
                                   ProgressController.Factory animationControllerFactory,
                                   ImageOperations imageOperations) {
        this.artworkView = artworkView;
        this.imageOperations = imageOperations;
        wrappedImageView = (ImageView) artworkView.findViewById(R.id.artwork_image_view);
        progressController = animationControllerFactory.create(wrappedImageView);
        artworkView.setOnWidthChangedListener(this);
    }

    @Override
    public void setProgress(PlaybackProgress progress) {
        latestProgress = progress;
        if (!suppressProgress) {
            progressController.setPlaybackProgress(progress);
        }
    }

    public void showPlayingState(PlaybackProgress progress) {
        isPlaying = true;
        if (progress != null && !suppressProgress) {
            progressController.startProgressAnimation(progress);
        }
    }

    public void showIdleState() {
        isPlaying = false;
        progressController.cancelProgressAnimation();
    }

    @Override
    public void scrubStateChanged(int newScrubState) {
        suppressProgress = newScrubState == SCRUB_STATE_SCRUBBING;
        if (suppressProgress) {
            progressController.cancelProgressAnimation();
        }
        if (newScrubState == SCRUB_STATE_CANCELLED && isPlaying) {
            progressController.startProgressAnimation(latestProgress);
        }
    }

    @Override
    public void displayScrubPosition(float scrubPosition) {
        if (helper != null) {
            helper.setValueFromProportion(wrappedImageView, scrubPosition);
        }
    }

    @Override
    public void onArtworkSizeChanged() {
        configureBounds();
    }

    @Override
    public void onLoadingStarted(String imageUri, View view) {
        // no-op
    }

    @Override
    public void onLoadingFailed(String imageUri, View view, String failedReason) {
        // no-op
    }

    @Override
    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
        configureBounds();
    }


    public void clear(){
        wrappedImageView.setImageDrawable(null);
    }

    public void loadArtwork(TrackUrn urn, boolean isHighPriority) {
        final Resources resources = wrappedImageView.getResources();
        final ApiImageSize size = ApiImageSize.getFullImageSize(resources);
        final Bitmap cachedListBitmap = imageOperations.getCachedListItemBitmap(resources, urn);
        imageOperations.displayInPlayer(urn, size, wrappedImageView, this, cachedListBitmap, isHighPriority);
    }

    public void reset() {
        progressController.reset();
        clear();
    }

    private void configureBounds() {
        final int width = artworkView.getWidth();
        final int imageViewWidth = wrappedImageView.getMeasuredWidth();

        if (width > 0 && imageViewWidth > 0) {
            helper = new TranslateXHelper(0, Math.min(0, -(imageViewWidth - width)));
            progressController.setHelper(helper);
        }
    }

    public static class Factory {
        private final ProgressController.Factory animationControllerFactory;
        private final ImageOperations imageOperations;

        @Inject
        Factory(ProgressController.Factory animationControllerFactory, ImageOperations imageOperations) {
            this.animationControllerFactory = animationControllerFactory;
            this.imageOperations = imageOperations;
        }

        public PlayerArtworkController create(PlayerTrackArtworkView artworkView) {
            return new PlayerArtworkController(artworkView, animationControllerFactory,
                    imageOperations);
        }
    }
}
