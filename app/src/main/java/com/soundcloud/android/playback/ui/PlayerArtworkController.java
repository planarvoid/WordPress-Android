package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.ui.progress.ScrubController.OnScrubListener;
import static com.soundcloud.android.playback.ui.progress.ScrubController.SCRUB_STATE_CANCELLED;
import static com.soundcloud.android.playback.ui.progress.ScrubController.SCRUB_STATE_SCRUBBING;
import static com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView.OnWidthChangedListener;

import com.nineoldandroids.view.ViewHelper;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.playback.ui.progress.ProgressController;
import com.soundcloud.android.playback.ui.progress.TranslateXHelper;
import com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView;
import com.soundcloud.android.tracks.TrackUrn;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;

public class PlayerArtworkController implements ProgressAware, OnScrubListener, OnWidthChangedListener, ImageListener {
    private final PlayerTrackArtworkView artworkView;
    private final ImageView wrappedImageView;
    private final ImageView imageOverlay;
    private final ProgressController progressController;
    private final PlayerArtworkLoader playerArtworkLoader;

    private TranslateXHelper helper;
    private boolean suppressProgress;

    private PlaybackProgress latestProgress = PlaybackProgress.empty();
    private boolean isPlaying;

    public PlayerArtworkController(PlayerTrackArtworkView artworkView,
                                   ProgressController.Factory animationControllerFactory,
                                   PlayerArtworkLoader playerArtworkLoader) {
        this.artworkView = artworkView;
        this.playerArtworkLoader = playerArtworkLoader;
        wrappedImageView = (ImageView) artworkView.findViewById(R.id.artwork_image_view);
        imageOverlay = (ImageView) artworkView.findViewById(R.id.artwork_overlay_image);
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
        if (helper != null){
            ViewHelper.setTranslationX(imageOverlay, ViewHelper.getTranslationX(wrappedImageView));
        }

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
            helper.setValueFromProportion(imageOverlay, scrubPosition);
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
        imageOverlay.setImageDrawable(null);
    }

    public void loadArtwork(TrackUrn urn, boolean isHighPriority) {
        playerArtworkLoader.loadArtwork(urn, wrappedImageView, imageOverlay, this, isHighPriority);
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
        private final PlayerArtworkLoader playerArtworkLoader;

        @Inject
        Factory(ProgressController.Factory animationControllerFactory, PlayerArtworkLoader playerArtworkLoader) {
            this.animationControllerFactory = animationControllerFactory;
            this.playerArtworkLoader = playerArtworkLoader;
        }

        public PlayerArtworkController create(PlayerTrackArtworkView artworkView) {
            return new PlayerArtworkController(artworkView, animationControllerFactory,
                    playerArtworkLoader);
        }
    }

}
