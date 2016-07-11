package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.playback.ui.progress.ProgressController;
import com.soundcloud.android.playback.ui.progress.TranslateXHelper;
import com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import org.jetbrains.annotations.NotNull;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import javax.inject.Inject;

class PlayQueueArtworkController implements ProgressAware, PlayerTrackArtworkView.OnWidthChangedListener {

    private final BlurringPlayQueueArtworkLoader playerArtworkLoader;
    private final PlayQueueOperations playQueueOperations;
    private final ProgressController.Factory progressControllerFactory;

    private Subscription subscription = RxUtils.invalidSubscription();
    private PlayerTrackArtworkView artworkImageView;
    private ProgressController progressController;

    @Inject
    public PlayQueueArtworkController(BlurringPlayQueueArtworkLoader playerArtworkLoader,
                                      PlayQueueOperations playQueueOperations,
                                      ProgressController.Factory progressControllerFactory) {
        this.playerArtworkLoader = playerArtworkLoader;
        this.playQueueOperations = playQueueOperations;
        this.progressControllerFactory = progressControllerFactory;
    }

    public void loadArtwork(Urn trackUrn) {
        subscription.unsubscribe();
        clearProgress();
        subscription = playQueueOperations.getTrackArtworkResource(trackUrn)
                                          .observeOn(AndroidSchedulers.mainThread())
                                          .subscribe(new LoadArtworkSubscriber());
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

    private class LoadArtworkSubscriber extends DefaultSubscriber<ImageResource> {
        @Override
        public void onNext(ImageResource imageResource) {
            playerArtworkLoader.loadArtwork(imageResource, artworkImageView.getWrappedImageView());
        }
    }
}
