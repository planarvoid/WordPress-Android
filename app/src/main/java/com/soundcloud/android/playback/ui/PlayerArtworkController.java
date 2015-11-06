package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.ui.progress.ScrubController.OnScrubListener;
import static com.soundcloud.android.playback.ui.progress.ScrubController.SCRUB_STATE_CANCELLED;
import static com.soundcloud.android.playback.ui.progress.ScrubController.SCRUB_STATE_SCRUBBING;
import static com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView.OnWidthChangedListener;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.playback.ui.progress.ProgressController;
import com.soundcloud.android.playback.ui.progress.TranslateXHelper;
import com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView;

import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;
import javax.inject.Provider;

public class PlayerArtworkController implements ProgressAware, OnScrubListener, OnWidthChangedListener {
    private final PlayerTrackArtworkView artworkView;
    private final ImageView wrappedImageView;
    private final ImageView imageOverlay;
    private final View imageHolder;
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
        imageHolder = artworkView.findViewById(R.id.artwork_holder);
        progressController = animationControllerFactory.create(imageHolder);
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

    public void showIdleState(PlaybackProgress progress) {
        showIdleState();
        if (!progress.isEmpty()){
            setProgress(progress);
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
            helper.setValueFromProportion(imageHolder, scrubPosition);
        }
    }

    @Override
    public void onArtworkSizeChanged() {
        final int width = artworkView.getWidth();
        final int imageViewWidth = wrappedImageView.getMeasuredWidth();

        if (width > 0 && imageViewWidth > 0) {
            helper = new TranslateXHelper(0, Math.min(0, -(imageViewWidth - width)));
            progressController.setHelper(helper);

        }
    }

    public void clear(){
        wrappedImageView.setImageDrawable(null);
        imageOverlay.setImageDrawable(null);
    }

    public void loadArtwork(Urn urn, boolean isHighPriority, ViewVisibilityProvider viewVisibilityProvider) {
        playerArtworkLoader.loadArtwork(urn, wrappedImageView, imageOverlay, isHighPriority, viewVisibilityProvider);
    }

    public void reset() {
        progressController.reset();
        clear();
    }

    public void cancelProgressAnimations() {
        progressController.cancelProgressAnimation();
    }


    public static class Factory {
        private final ProgressController.Factory animationControllerFactory;
        private final Provider<PlayerArtworkLoader> playerArtworkLoaderProvider;

        @Inject
        Factory(ProgressController.Factory animationControllerFactory, Provider<PlayerArtworkLoader> playerArtworkLoaderProvider) {
            this.animationControllerFactory = animationControllerFactory;
            this.playerArtworkLoaderProvider = playerArtworkLoaderProvider;
        }

        public PlayerArtworkController create(PlayerTrackArtworkView artworkView) {
            return new PlayerArtworkController(artworkView, animationControllerFactory,
                    playerArtworkLoaderProvider.get());
        }
    }

}
