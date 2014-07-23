package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.ui.progress.ScrubController.OnScrubListener;
import static com.soundcloud.android.playback.ui.progress.ScrubController.SCRUB_STATE_CANCELLED;
import static com.soundcloud.android.playback.ui.progress.ScrubController.SCRUB_STATE_SCRUBBING;
import static com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView.OnWidthChangedListener;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.playback.ui.progress.ProgressController;
import com.soundcloud.android.playback.ui.progress.TranslateXHelper;
import com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;

public class PlayerArtworkController implements ProgressAware, OnScrubListener, OnWidthChangedListener, ImageListener {
    private final PlayerTrackArtworkView artworkView;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final ImageView wrappedImageView;
    private final ProgressController progressController;

    private TranslateXHelper helper;
    private boolean suppressProgress;

    private PlaybackProgress latestProgress = PlaybackProgress.empty();

    public PlayerArtworkController(PlayerTrackArtworkView artworkView,
                                   ProgressController.Factory animationControllerFactory,
                                   PlaySessionStateProvider playSessionStateProvider) {
        this.artworkView = artworkView;
        this.playSessionStateProvider = playSessionStateProvider;
        wrappedImageView = (ImageView) artworkView.findViewById(R.id.artwork_image_view);
        progressController = animationControllerFactory.create(wrappedImageView);
        artworkView.setOnWidthChangedListener(this);
    }

    public ImageListener getImageListener() {
        return this;
    }

    public void showPlayingState(PlaybackProgress progress) {
        showSessionActiveState();
        if (progress != null && !suppressProgress) {
            progressController.startProgressAnimation(progress);
        }
    }

    @Override
    public void setProgress(PlaybackProgress progress) {
        latestProgress = progress;
        if (!suppressProgress) {
            progressController.setPlaybackProgress(progress);
        }
    }

    public void showSessionActiveState() {
        progressController.cancelProgressAnimation();
    }

    public void showIdleState() {
        progressController.cancelProgressAnimation();
    }

    @Override
    public void scrubStateChanged(int newScrubState) {
        suppressProgress = newScrubState == SCRUB_STATE_SCRUBBING;
        if (suppressProgress) {
            progressController.cancelProgressAnimation();
        }
        if (newScrubState == SCRUB_STATE_CANCELLED && playSessionStateProvider.isPlaying()) {
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


    public ImageView getImageView() {
        return wrappedImageView;
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
        private final PlaySessionStateProvider playSessionStateProvider;

        @Inject
        Factory(ProgressController.Factory animationControllerFactory, PlaySessionStateProvider playSessionStateProvider) {
            this.animationControllerFactory = animationControllerFactory;
                this.playSessionStateProvider = playSessionStateProvider;
        }

        public PlayerArtworkController create(PlayerTrackArtworkView artworkView) {
            return new PlayerArtworkController(artworkView, animationControllerFactory,  playSessionStateProvider);
        }
    }
}
