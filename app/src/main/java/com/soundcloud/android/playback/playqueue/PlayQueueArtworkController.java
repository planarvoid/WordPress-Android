package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.playback.ui.progress.ProgressController;
import com.soundcloud.android.playback.ui.progress.TranslateXHelper;
import com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

class PlayQueueArtworkController
        implements ProgressAware, PlayerTrackArtworkView.OnWidthChangedListener, PlayQueueAdapter.NowPlayingListener {

    private final BlurringPlayQueueArtworkLoader playerArtworkLoader;
    private final ProgressController.Factory progressControllerFactory;

    private PlayerTrackArtworkView artworkImageView;
    private ProgressController progressController;

    @Inject
    public PlayQueueArtworkController(BlurringPlayQueueArtworkLoader playerArtworkLoader,
                                      ProgressController.Factory progressControllerFactory) {
        this.playerArtworkLoader = playerArtworkLoader;
        this.progressControllerFactory = progressControllerFactory;
    }

    @Override
    public void onNowPlayingChanged(TrackPlayQueueUIItem trackItem) {
        clearProgress();
        playerArtworkLoader.loadArtwork(trackItem.getImageResource(), artworkImageView.getWrappedImageView());
    }

    public void setPlayState(PlayStateEvent stateEvent) {
        if (stateEvent.isPlayerPlaying()) {
            progressController.startProgressAnimation(stateEvent.getProgress(), stateEvent.getProgress().getDuration());
        } else {
            progressController.cancelProgressAnimation();
        }
    }

    @Override
    public void setProgress(PlaybackProgress progress) {
        progressController.setPlaybackProgress(progress, progress.getDuration());
    }

    @Override
    public void clearProgress() {
        progressController.reset();
    }

    public void bind(@NotNull PlayerTrackArtworkView artworkImageView) {
        this.artworkImageView = artworkImageView;
        this.artworkImageView.setOnWidthChangedListener(this);
        this.progressController = progressControllerFactory.create(artworkImageView.getArtworkHolder());
    }

    @Override
    public void onArtworkSizeChanged() {
        final int width = artworkImageView.getWidth();
        final int imageViewWidth = artworkImageView.getWrappedImageView().getMeasuredWidth();

        if (width > 0 && imageViewWidth > 0) {
            TranslateXHelper helper = new TranslateXHelper(0, Math.min(0, -(imageViewWidth - width)));
            progressController.setHelper(helper);
        }
    }

}
