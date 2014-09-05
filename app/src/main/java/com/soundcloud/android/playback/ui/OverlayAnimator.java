package com.soundcloud.android.playback.ui;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.soundcloud.android.R;

import android.content.res.Resources;
import android.view.View;

import javax.inject.Inject;

public class OverlayAnimator {
    private final int fadeDuration;
    private ObjectAnimator objectAnimator;

    @Inject
    public OverlayAnimator(Resources resources) {
        fadeDuration = resources.getInteger(R.integer.overlay_animate_duration);
        // Required by Dagger
    }

    public void setAlpha(View artworkIdleOverlay, float alpha){
        stopOverlayAnimation();
        ViewHelper.setAlpha(artworkIdleOverlay, alpha);
    }

    public void showOverlay(View artworkIdleOverlay) {
        stopOverlayAnimation();
        objectAnimator = ObjectAnimator.ofFloat(artworkIdleOverlay, "alpha", ViewHelper.getAlpha(artworkIdleOverlay), 1f);
        objectAnimator.setDuration(fadeDuration);
        objectAnimator.start();
    }

    public void hideOverlay(View artworkIdleOverlay) {
        stopOverlayAnimation();
        objectAnimator = ObjectAnimator.ofFloat(artworkIdleOverlay, "alpha", ViewHelper.getAlpha(artworkIdleOverlay), 0f);
        objectAnimator.setDuration(fadeDuration);
        objectAnimator.start();
    }

    private void stopOverlayAnimation() {
        if (objectAnimator != null && objectAnimator.isRunning()) {
            objectAnimator.cancel();
        }
    }
}
