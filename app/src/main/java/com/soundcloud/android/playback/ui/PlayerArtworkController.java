package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.ui.progress.ScrubController.OnScrubListener;
import static com.soundcloud.android.playback.ui.progress.ScrubController.SCRUB_STATE_CANCELLED;
import static com.soundcloud.android.playback.ui.progress.ScrubController.SCRUB_STATE_SCRUBBING;
import static com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView.OnWidthChangedListener;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.playback.ui.progress.ProgressController;
import com.soundcloud.android.playback.ui.progress.TranslateXHelper;
import com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView;

import javax.inject.Inject;
import javax.inject.Provider;

public class PlayerArtworkController implements ProgressAware, OnScrubListener, OnWidthChangedListener {

    private final PlayerTrackArtworkView artworkView;
    private final ProgressController progressController;
    private final PlayerArtworkLoader playerArtworkLoader;

    private TranslateXHelper helper;
    private boolean suppressProgress;

    private PlaybackProgress latestProgress = PlaybackProgress.empty();
    private boolean isPlaying;
    private long fullDuration;

    public PlayerArtworkController(PlayerTrackArtworkView artworkView,
                                   ProgressController.Factory animationControllerFactory,
                                   PlayerArtworkLoader playerArtworkLoader) {
        this.artworkView = artworkView;
        this.playerArtworkLoader = playerArtworkLoader;
        progressController = animationControllerFactory.create(artworkView.getArtworkHolder());
        artworkView.setOnWidthChangedListener(this);
    }

    public void setFullDuration(long fullDuration) {
        this.fullDuration = fullDuration;
        if (!latestProgress.isEmpty()) {
            if (isPlaying) {
                showPlayingState(latestProgress);
            } else {
                setProgress(latestProgress);
            }
        }
    }

    @Override
    public void setProgress(PlaybackProgress progress) {
        latestProgress = progress;
        if (!suppressProgress && fullDuration > 0) {
            progressController.setPlaybackProgress(progress, fullDuration);
        }
    }

    @Override
    public void clearProgress() {
        setProgress(PlaybackProgress.empty());
    }

    public void setPlayState(PlayStateEvent state, boolean isCurrentTrack) {
        if (state.playSessionIsActive() && isCurrentTrack) {
            if (state.isPlayerPlaying()) {
                showPlayingState(state.getProgress());
            } else {
                showIdleState(isBufferingFromStart(state) ? latestProgress : state.getProgress());
            }
        } else {
            showIdleState();
        }

        artworkView.setArtworkActive(state.playSessionIsActive());
    }

    // Buffering events might have position 0 after skippy has been reinitialized
    private boolean isBufferingFromStart(PlayStateEvent state) {
        boolean isSameTrack = latestProgress.getUrn().equals(state.getPlayingItemUrn());
        return state.isBuffering() && isSameTrack && state.getProgress().getPosition() == 0;
    }

    private void showPlayingState(PlaybackProgress progress) {
        latestProgress = progress;
        isPlaying = true;
        if (progress != null && !suppressProgress && fullDuration > 0) {
            progressController.startProgressAnimation(progress, fullDuration);
        }
    }

    private void showIdleState(PlaybackProgress progress) {
        latestProgress = progress;
        showIdleState();
        if (!progress.isEmpty()) {
            setProgress(progress);
        }
    }

    private void showIdleState() {
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
            progressController.startProgressAnimation(latestProgress, fullDuration);
        }
    }

    @Override
    public void displayScrubPosition(float actualPosition, float boundedPosition) {
        if (helper != null) {
            helper.setValueFromProportion(artworkView.getArtworkHolder(), boundedPosition);
        }
    }

    @Override
    public void onArtworkSizeChanged() {
        final int width = artworkView.getWidth();
        final int imageViewWidth = artworkView.getWrappedImageView().getMeasuredWidth();

        if (width > 0 && imageViewWidth > 0) {
            helper = new TranslateXHelper(0, Math.min(0, -(imageViewWidth - width)));
            progressController.setHelper(helper);

        }
    }

    public void clear() {
        artworkView.getWrappedImageView().setImageDrawable(null);
        artworkView.getImageOverlay().setImageDrawable(null);
    }

    public void loadArtwork(ImageResource imageResource,
                            boolean isHighPriority,
                            ViewVisibilityProvider viewVisibilityProvider) {
        playerArtworkLoader.loadArtwork(imageResource,
                                        artworkView.getWrappedImageView(),
                                        artworkView.getImageOverlay(),
                                        isHighPriority,
                                        viewVisibilityProvider);
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
        Factory(ProgressController.Factory animationControllerFactory,
                Provider<PlayerArtworkLoader> playerArtworkLoaderProvider) {
            this.animationControllerFactory = animationControllerFactory;
            this.playerArtworkLoaderProvider = playerArtworkLoaderProvider;
        }

        public PlayerArtworkController create(PlayerTrackArtworkView artworkView) {
            return new PlayerArtworkController(artworkView, animationControllerFactory,
                                               playerArtworkLoaderProvider.get());
        }
    }

}
