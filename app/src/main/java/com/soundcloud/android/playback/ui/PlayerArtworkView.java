package com.soundcloud.android.playback.ui;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.playback.ui.progress.ProgressController;
import com.soundcloud.android.playback.ui.progress.TranslateXHelper;
import com.soundcloud.android.view.AspectRatioImageView;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import javax.annotation.Nullable;

public class PlayerArtworkView extends FrameLayout implements ProgressAware, ImageListener {

    private static final int FADE_DURATION = 120;

    private final AspectRatioImageView wrappedImageView;
    private final ProgressController progressController;
    private final View artworkIdleOverlay;
    private ObjectAnimator overlayAnimator;
    private boolean isForcingDarkness;
    private boolean isInPlayingState;

    public PlayerArtworkView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.player_artwork_view, this);

        wrappedImageView = (AspectRatioImageView) findViewById(R.id.artwork_image_view);
        artworkIdleOverlay = findViewById(R.id.artwork_overlay);
        progressController = new ProgressController(wrappedImageView);
    }

    public ImageListener getImageListener() {
        return this;
    }

    public void showPlayingState(@Nullable PlaybackProgress progress) {
        isInPlayingState = true;
        if (!isForcingDarkness){
            hideOverlay();
        }

        if (progress != null){
            progressController.startProgressAnimation(progress);
        }
    }

    public void showIdleState() {
        isInPlayingState = false;
        progressController.cancelProgressAnimation();
        showOverlay();
    }

    @Override
    public void setProgress(PlaybackProgress progress) {
        progressController.setPlaybackProgress(progress);
    }

    public void darken(){
        isForcingDarkness = true;
        showOverlay();
    }

    public void lighten(){
        isForcingDarkness = false;
        if (isInPlayingState){
            hideOverlay();
        }
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
        if (overlayAnimator != null && overlayAnimator.isRunning()){
            overlayAnimator.cancel();
        }
    }

    public ImageView getImageView() {
        return wrappedImageView;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        configureBounds();
    }

    private void configureBounds(){
        final int width = getWidth();
        final int imageViewWidth = wrappedImageView.getMeasuredWidth();

        if (width > 0 && imageViewWidth > 0){
            progressController.setHelper(new TranslateXHelper(0, Math.min(0, -(imageViewWidth - width))));
        }
    }

    @Override
    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
        configureBounds();
    }

    @Override
    public void onLoadingStarted(String imageUri, View view) {
        // Nothing to do
    }

    @Override
    public void onLoadingFailed(String imageUri, View view, String failedReason) {
        // Nothing to do
    }

}