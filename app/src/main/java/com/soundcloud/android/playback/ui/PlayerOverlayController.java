package com.soundcloud.android.playback.ui;

import com.soundcloud.android.playback.PlaySessionStateProvider;

import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;

class PlayerOverlayController {

    private final OverlayAnimator overlayAnimator;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final View overlay;
    private boolean isCollapsed;

    @Inject
    public PlayerOverlayController(View overlay,
                                   OverlayAnimator overlayAnimator,
                                   PlaySessionStateProvider playSessionStateProvider) {
        this.overlay = overlay;
        this.overlayAnimator = overlayAnimator;
        this.playSessionStateProvider = playSessionStateProvider;
    }

    public void setCollapsedAndUpdate() {
        isCollapsed = true;
        update();
    }

    public void setExpandedAndUpdate() {
        isCollapsed = false;
        update();
    }

    public void update() {
        if (!isCollapsed && playSessionStateProvider.isPlaying()) {
            overlayAnimator.hideOverlay(overlay);
        } else {
            overlayAnimator.showOverlay(overlay);
        }
    }

    public static class Factory {
        private final Provider<OverlayAnimator> overlayAnimatorProvider;
        private final PlaySessionStateProvider playSessionStateProvider;

        @Inject
        Factory(Provider<OverlayAnimator> overlayAnimatorProvider, PlaySessionStateProvider playSessionController) {
            this.overlayAnimatorProvider = overlayAnimatorProvider;
            this.playSessionStateProvider = playSessionController;
        }

        public PlayerOverlayController create(View overlay) {
            return new PlayerOverlayController(overlay, overlayAnimatorProvider.get(), playSessionStateProvider);
        }
    }
}
