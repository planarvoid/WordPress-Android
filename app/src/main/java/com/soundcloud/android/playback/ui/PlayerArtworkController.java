package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.ui.PlayerArtworkView.OnWidthChangedListener;
import static com.soundcloud.android.playback.ui.progress.ProgressController.ProgressAnimationControllerFactory;
import static com.soundcloud.android.playback.ui.progress.ScrubController.OnScrubListener;
import static com.soundcloud.android.playback.ui.progress.ScrubController.SCRUB_STATE_SCRUBBING;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.playback.ui.progress.ProgressController;
import com.soundcloud.android.playback.ui.progress.TranslateXHelper;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;

public class PlayerArtworkController implements ProgressAware, OnScrubListener, OnWidthChangedListener, ImageListener {

    private static final int FADE_DURATION = 120;

    private final PlayerArtworkView artworkView;
    private final ImageView wrappedImageView;
    private final ProgressController progressController;
    private final View artworkIdleOverlay;

    private ObjectAnimator overlayAnimator;
    private TranslateXHelper helper;
    private boolean isForcingDarkness;
    private boolean isInPlayingState;
    private boolean suppressProgress;

    public PlayerArtworkController(PlayerArtworkView artworkView, ProgressAnimationControllerFactory animationControllerFactory) {
        this.artworkView = artworkView;
        wrappedImageView = (ImageView) artworkView.findViewById(R.id.artwork_image_view);
        progressController = animationControllerFactory.create(wrappedImageView);
        artworkIdleOverlay = artworkView.findViewById(R.id.artwork_overlay);
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

    public void showSessionActiveState() {
        isInPlayingState = true;
        progressController.cancelProgressAnimation();
        if (!isForcingDarkness) {
            hideOverlay();
        }
    }

    public void showIdleState() {
        isInPlayingState = false;
        progressController.cancelProgressAnimation();
        showOverlay();
    }

    @Override
    public void setProgress(PlaybackProgress progress) {
        if (!suppressProgress) {
            progressController.setPlaybackProgress(progress);
        }
    }

    public void darken() {
        isForcingDarkness = true;
        showOverlay();
    }

    public void lighten() {
        isForcingDarkness = false;
        if (isInPlayingState) {
            hideOverlay();
        }
    }

    @Override
    public void scrubStateChanged(int newScrubState) {
        suppressProgress = newScrubState == SCRUB_STATE_SCRUBBING;
        if (suppressProgress){
            progressController.cancelProgressAnimation();
            darken();
        } else {
            lighten();
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

    private void showOverlay() {
        stopOverlayAnimation();
        overlayAnimator = ObjectAnimator.ofFloat(artworkIdleOverlay, "alpha", ViewHelper.getAlpha(artworkIdleOverlay), 1);
        overlayAnimator.setDuration(FADE_DURATION);
        overlayAnimator.start();
    }

    private void hideOverlay() {
        stopOverlayAnimation();
        overlayAnimator = ObjectAnimator.ofFloat(artworkIdleOverlay, "alpha", ViewHelper.getAlpha(artworkIdleOverlay), 0);
        overlayAnimator.setDuration(FADE_DURATION);
        overlayAnimator.start();
    }

    private void stopOverlayAnimation() {
        if (overlayAnimator != null && overlayAnimator.isRunning()) {
            overlayAnimator.cancel();
        }
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

    public static class PlayerArtworkControllerFactory {
        private final ProgressAnimationControllerFactory animationControllerFactory;

        @Inject
        PlayerArtworkControllerFactory(ProgressAnimationControllerFactory animationControllerFactory) {
            this.animationControllerFactory = animationControllerFactory;
        }
        public PlayerArtworkController create(PlayerArtworkView artworkView){
            return new PlayerArtworkController(artworkView, animationControllerFactory);
        }
    }
}
