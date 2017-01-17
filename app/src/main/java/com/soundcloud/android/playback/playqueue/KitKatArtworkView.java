package com.soundcloud.android.playback.playqueue;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressController;
import com.soundcloud.android.playback.ui.progress.TranslateXHelper;
import com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

public class KitKatArtworkView extends ArtworkView implements PlayerTrackArtworkView.OnWidthChangedListener {

    private final ArtworkPresenter artworkPresenter;
    private final ProgressController.Factory progressControllerFactory;
    private final BlurringPlayQueueArtworkLoader playerArtworkLoader;

    @BindView(R.id.artwork_view) PlayerTrackArtworkView artWorkView;
    private Unbinder unbinder;
    private ProgressController progressController;

    @Inject
    public KitKatArtworkView(ArtworkPresenter artworkPresenter,
                             ProgressController.Factory progressControllerFactory,
                             BlurringPlayQueueArtworkLoader playerArtworkLoader) {
        this.artworkPresenter = artworkPresenter;
        this.progressControllerFactory = progressControllerFactory;
        this.playerArtworkLoader = playerArtworkLoader;
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        unbinder = ButterKnife.bind(this, view);
        artWorkView.setOnWidthChangedListener(this);
        progressController = progressControllerFactory.create(artWorkView.getArtworkHolder());
        artworkPresenter.attachView(this);
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        super.onDestroyView(fragment);
        unbinder.unbind();
        artworkPresenter.detachView();
    }

    @Override
    public void setImage(ImageResource imageResource) {
        playerArtworkLoader.loadArtwork(imageResource, artWorkView.getWrappedImageView());
    }

    @Override
    public void cancelProgressAnimation() {
        progressController.cancelProgressAnimation();
    }

    @Override
    public void startProgressAnimation(PlaybackProgress progress, long duration) {
        progressController.startProgressAnimation(progress, duration);
    }

    @Override
    public void setPlaybackProgress(PlaybackProgress progress, long duration) {
        progressController.setPlaybackProgress(progress, duration);
    }

    @Override
    public void setProgressControllerValues(int startX, int endX) {
        TranslateXHelper helper = new TranslateXHelper(startX, endX);
        progressController.setHelper(helper);
    }

    @Override
    public void onArtworkSizeChanged() {
        final int width = artWorkView.getWidth();
        final int imageViewWidth = artWorkView.getWrappedImageView().getMeasuredWidth();
        artworkPresenter.artworkSizeChanged(width, imageViewWidth);
    }
}
