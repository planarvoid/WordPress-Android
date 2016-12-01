package com.soundcloud.android.playback.ui;

import android.util.Pair;
import android.view.View;

import com.soundcloud.android.cast.CastPlayerStripController;

class SlideAnimationHelper {

    private static final Pair<Float, Float> SLIDE_TRANSITION_BOUNDS_ARTWORK = new Pair<>(.4f, 1f);
    private static final Pair<Float, Float> SLIDE_TRANSITION_BOUNDS_FOOTER = new Pair<>(.6f, 1f);
    private static final Pair<Float, Float> SLIDE_TRANSITION_BOUNDS_FULLSCREEN = new Pair<>(.4f, .9f);

    void configureViewsFromSlide(float slideOffset,
                                 View footerView,
                                 Iterable<View> fullscreenViews,
                                 Iterable<View> fullyHideOnCollapseViews,
                                 CastPlayerStripController castPanelController,
                                 PlayerOverlayController... overlayController) {
        configureViewsFromSlide(slideOffset, footerView, overlayController);
        final float alpha = getSlideAnimateValue(slideOffset, SLIDE_TRANSITION_BOUNDS_FULLSCREEN);
        setAlpha(alpha, fullscreenViews);
        setVisibility(alpha < 0.001, fullyHideOnCollapseViews);
        castPanelController.setHeightFromCollapse(alpha);
    }

    void configureViewsFromSlide(float slideOffset,
                                 View footerView,
                                 View fullscreenView,
                                 PlayerOverlayController... overlayController) {
        configureViewsFromSlide(slideOffset, footerView, overlayController);
        setAlpha(getSlideAnimateValue(slideOffset, SLIDE_TRANSITION_BOUNDS_FULLSCREEN), fullscreenView);
    }

    private void configureViewsFromSlide(float slideOffset,
                                         View footerView,
                                         PlayerOverlayController... overlayControllers) {
        for (PlayerOverlayController overlayController : overlayControllers) {
            overlayController.setAlphaFromCollapse(getSlideAnimateValue(1 - slideOffset,
                    SLIDE_TRANSITION_BOUNDS_ARTWORK));
        }
        setAlpha(getSlideAnimateValue(1 - slideOffset, SLIDE_TRANSITION_BOUNDS_FOOTER), footerView);
    }

    private void setAlpha(float alpha, Iterable<View> fullscreenViews) {
        for (View v : fullscreenViews) {
            setAlpha(alpha, v);
        }
    }

    private void setAlpha(float alpha, View view) {
        final float adjustedAlpha = Math.min(1.0f, Math.max(0.0f, alpha));
        view.setAlpha(adjustedAlpha);
    }

    private void setVisibility(boolean shouldHide, Iterable<View> fullyHideOnCollapseViews) {
        for (View v : fullyHideOnCollapseViews) {
            if (shouldHide) {
                v.setVisibility(View.INVISIBLE);
            } else {
                v.setVisibility(View.VISIBLE);
            }
        }
    }

    private float getSlideAnimateValue(float slideOffset, Pair<Float, Float> bounds) {
        return Math.min(1.0f, Math.max(0.0f, (slideOffset - bounds.first) / (bounds.second - bounds.first)));
    }

}
