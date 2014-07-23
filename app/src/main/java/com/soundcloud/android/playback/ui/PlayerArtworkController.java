package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.ui.progress.ProgressController.ProgressAnimationControllerFactory;
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
import javax.inject.Provider;

public class PlayerArtworkController implements ProgressAware, OnScrubListener, OnWidthChangedListener, ImageListener {
    private final PlayerTrackArtworkView artworkView;
    private final PlayerOverlayController playerOverlayController;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final ImageView wrappedImageView;
    private final ProgressController progressController;
    private final View artworkIdleOverlay;

    private TranslateXHelper helper;
    private boolean suppressProgress;

    private PlaybackProgress latestProgress = PlaybackProgress.empty();

    public PlayerArtworkController(PlayerTrackArtworkView artworkView, ProgressAnimationControllerFactory animationControllerFactory,
                                   PlayerOverlayController playerOverlayController, PlaySessionStateProvider playSessionStateProvider) {
        this.artworkView = artworkView;
        this.playerOverlayController = playerOverlayController;
        this.playSessionStateProvider = playSessionStateProvider;
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

    @Override
    public void setProgress(PlaybackProgress progress) {
        latestProgress = progress;
        if (!suppressProgress) {
            progressController.setPlaybackProgress(progress);
        }
    }

    public void showSessionActiveState() {
        progressController.cancelProgressAnimation();
        playerOverlayController.showSessionActiveState(artworkIdleOverlay);
    }

    public void showIdleState() {
        progressController.cancelProgressAnimation();
        playerOverlayController.showIdleState(artworkIdleOverlay);
    }

    @Override
    public void scrubStateChanged(int newScrubState) {
        suppressProgress = newScrubState == SCRUB_STATE_SCRUBBING;
        if (suppressProgress) {
            progressController.cancelProgressAnimation();
            playerOverlayController.darken(artworkIdleOverlay);
        } else {
            hideOverlay();
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

    public void hideOverlay() {
        playerOverlayController.hideOverlay(artworkIdleOverlay);
    }

    public void darken() {
        playerOverlayController.darken(artworkIdleOverlay);
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
        private final Provider<PlayerOverlayController> overlayControllerProvider;
        private final PlaySessionStateProvider playSessionStateProvider;

        @Inject
        PlayerArtworkControllerFactory(ProgressAnimationControllerFactory animationControllerFactory,
                                       Provider<PlayerOverlayController> overlayControllerProvider,
                                       PlaySessionStateProvider playSessionStateProvider) {
            this.animationControllerFactory = animationControllerFactory;
            this.overlayControllerProvider = overlayControllerProvider;
            this.playSessionStateProvider = playSessionStateProvider;
        }

        public PlayerArtworkController create(PlayerTrackArtworkView artworkView) {
            return new PlayerArtworkController(artworkView, animationControllerFactory, overlayControllerProvider.get(),
                    playSessionStateProvider);
        }
    }
}
