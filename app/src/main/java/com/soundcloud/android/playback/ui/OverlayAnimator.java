package com.soundcloud.android.playback.ui;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

import android.view.View;

import javax.inject.Inject;

public class OverlayAnimator {
    private static final int FADE_DURATION = 120;
    private ObjectAnimator objectAnimator;

    @Inject
    public OverlayAnimator() {
        // Required by Dagger
    }

    public void setAlpha(View artworkIdleOverlay, float alpha){
        stopOverlayAnimation();
        ViewHelper.setAlpha(artworkIdleOverlay, alpha);
    }

    public void showOverlay(View artworkIdleOverlay) {
        stopOverlayAnimation();
        objectAnimator = ObjectAnimator.ofFloat(artworkIdleOverlay, "alpha", ViewHelper.getAlpha(artworkIdleOverlay), 1f);
        objectAnimator.setDuration(FADE_DURATION);
        objectAnimator.start();
    }

    public void hideOverlay(View artworkIdleOverlay) {
        stopOverlayAnimation();
        objectAnimator = ObjectAnimator.ofFloat(artworkIdleOverlay, "alpha", ViewHelper.getAlpha(artworkIdleOverlay), 0f);
        objectAnimator.setDuration(FADE_DURATION);
        objectAnimator.start();
    }

    private void stopOverlayAnimation() {
        if (objectAnimator != null && objectAnimator.isRunning()) {
            objectAnimator.cancel();
        }
    }
}
