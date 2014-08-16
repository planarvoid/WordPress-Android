package com.soundcloud.android.playback.ui;

import com.nineoldandroids.view.ViewHelper;

import android.util.Pair;
import android.view.View;

class SlideAnimationHelper {

    private static final Pair<Float, Float> SLIDE_TRANSITION_BOUNDS_ARTWORK = new Pair<Float, Float>(.4f, 1f);
    private static final Pair<Float, Float> SLIDE_TRANSITION_BOUNDS_FOOTER = new Pair<Float, Float>(.6f, 1f);
    private static final Pair<Float, Float> SLIDE_TRANSITION_BOUNDS_FULLSCREEN = new Pair<Float, Float>(.4f, .9f);

    void configureViewsFromSlide(float slideOffset, PlayerOverlayController overlayController, View footerView,
                                 Iterable<View> fullscreenViews) {

        configureCollapsedViews(overlayController, footerView, 1 - slideOffset);
        setAlpha(getSlideAnimateValue(slideOffset, SLIDE_TRANSITION_BOUNDS_FULLSCREEN), fullscreenViews);
    }

    private void configureCollapsedViews(PlayerOverlayController overlayController, View footerView, float invertedSlideOffset) {
        overlayController.setAlphaFromCollapse(getSlideAnimateValue(invertedSlideOffset, SLIDE_TRANSITION_BOUNDS_ARTWORK));
        setAlpha(getSlideAnimateValue(invertedSlideOffset, SLIDE_TRANSITION_BOUNDS_FOOTER), footerView);
    }

    private void setAlpha(float alpha, Iterable<View> views) {
        for (View v : views) {
            setAlpha(alpha, v);
        }
    }

    private void setAlpha(float alpha, View view) {
        final float adjustedAlpha = Math.min(1.0f, Math.max(0.0f, alpha));
        ViewHelper.setAlpha(view, adjustedAlpha);
        view.setVisibility(adjustedAlpha > 0 ? View.VISIBLE : View.GONE);
    }

    private float getSlideAnimateValue(float slideOffset, Pair<Float, Float> bounds) {
        return Math.min(1.0f, Math.max(0.0f, (slideOffset - bounds.first) / (bounds.second - bounds.first)));
    }

}
