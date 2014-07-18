package com.soundcloud.android.playback.ui;

import com.soundcloud.android.playback.PlaySessionController;

import android.view.View;

import javax.inject.Inject;

class PlayerOverlayController {

    private final OverlayAnimator overlayAnimator;
    private final PlaySessionController playSessionController;

    private boolean isCollapsed;

    @Inject
    public PlayerOverlayController(OverlayAnimator overlayAnimator, PlaySessionController playSessionController) {
        this.overlayAnimator = overlayAnimator;
        this.playSessionController = playSessionController;
    }

    public void showSessionActiveState(View overlay) {
        if (!isCollapsed) {
            overlayAnimator.hideOverlay(overlay);
        }
    }

    public void hideOverlay(View overlay) {
        isCollapsed = false;
        if (playSessionController.isPlaying()) {
            overlayAnimator.hideOverlay(overlay);
        }
    }

    public void showIdleState(View overlay) {
        overlayAnimator.showOverlay(overlay);
    }

    public void darken(View overlay) {
        isCollapsed = true;
        overlayAnimator.showOverlay(overlay);
    }
}
