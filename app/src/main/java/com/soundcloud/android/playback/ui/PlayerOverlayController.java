package com.soundcloud.android.playback.ui;

import com.soundcloud.android.playback.PlaySessionStateProvider;

import android.view.View;

import javax.inject.Inject;

class PlayerOverlayController {

    private final OverlayAnimator overlayAnimator;
    private final PlaySessionStateProvider playSessionStateProvider;

    private boolean isCollapsed;

    @Inject
    public PlayerOverlayController(OverlayAnimator overlayAnimator, PlaySessionStateProvider playSessionStateProvider) {
        this.overlayAnimator = overlayAnimator;
        this.playSessionStateProvider = playSessionStateProvider;
    }

    public void showSessionActiveState(View overlay) {
        if (!isCollapsed) {
            overlayAnimator.hideOverlay(overlay);
        }
    }

    public void hideOverlay(View overlay) {
        isCollapsed = false;
        if (playSessionStateProvider.isPlaying()) {
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
