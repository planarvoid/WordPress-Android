package com.soundcloud.android.playback.ui;

import android.util.Pair;
import android.view.View;

class SlideAnimationHelper {

    private static final Pair<Float, Float> SLIDE_TRANSITION_BOUNDS_ARTWORK = new Pair<>(.4f, 1f);
    private static final Pair<Float, Float> SLIDE_TRANSITION_BOUNDS_FOOTER = new Pair<>(.6f, 1f);
    private static final Pair<Float, Float> SLIDE_TRANSITION_BOUNDS_FULLSCREEN = new Pair<>(.4f, .9f);

    void configureViewsFromSlide(float slideOffset, View footerView, Iterable<View> fullscreenViews, PlayerOverlayController... overlayController) {
        configureViewsFromSlide(slideOffset, footerView, overlayController);
        setAlpha(getSlideAnimateValue(slideOffset, SLIDE_TRANSITION_BOUNDS_FULLSCREEN), fullscreenViews);
    }

    void configureViewsFromSlide(float slideOffset, View footerView, View fullscreenView, PlayerOverlayController... overlayController) {
        configureViewsFromSlide(slideOffset, footerView, overlayController);
        setAlpha(getSlideAnimateValue(slideOffset, SLIDE_TRANSITION_BOUNDS_FULLSCREEN), fullscreenView);
    }

    private void configureViewsFromSlide(float slideOffset, View footerView, PlayerOverlayController... overlayControllers) {
        for (PlayerOverlayController overlayController : overlayControllers){
            overlayController.setAlphaFromCollapse(getSlideAnimateValue(1 - slideOffset, SLIDE_TRANSITION_BOUNDS_ARTWORK));
        }
        setAlpha(getSlideAnimateValue(1 - slideOffset, SLIDE_TRANSITION_BOUNDS_FOOTER), footerView);
    }

    private void setAlpha(float alpha, Iterable<View> views) {
        for (View v : views) {
            setAlpha(alpha, v);
        }
    }

    private void setAlpha(float alpha, View view) {
        final float adjustedAlpha = Math.min(1.0f, Math.max(0.0f, alpha));
        view.setAlpha(adjustedAlpha);
    }

    private float getSlideAnimateValue(float slideOffset, Pair<Float, Float> bounds) {
        return Math.min(1.0f, Math.max(0.0f, (slideOffset - bounds.first) / (bounds.second - bounds.first)));
    }

}
