package com.soundcloud.android.playback.ui;

import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.ui.progress.ScrubController;

import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;

class PlayerOverlayController implements ScrubController.OnScrubListener {

    private final OverlayAnimator overlayAnimator;
    private final View overlay;
    private boolean isScrubbing;
    private float alphaFromCollapse;

    private boolean playSessionIsActive;
    private boolean adOverlayShown;

    public PlayerOverlayController(View overlay, OverlayAnimator overlayAnimator) {
        this.overlay = overlay;
        this.overlayAnimator = overlayAnimator;
    }

    public void setPlayState(Player.StateTransition stateTransition) {
        playSessionIsActive = stateTransition.playSessionIsActive();
        configureOverlay();
    }

    public void setAdOverlayShown(boolean isShown) {
        adOverlayShown = isShown;
        configureOverlay();
    }

    private void configureOverlay() {
        if (notScrubbing() && isExpanded() && playingAndNotShowingAd()) {
            overlayAnimator.hideOverlay(overlay);
        } else if (isExpanded()) {
            overlayAnimator.showOverlay(overlay);

        }
    }

    public void setAlphaFromCollapse(float alpha) {
        alphaFromCollapse = alpha;

        if (playingAndNotShowingAd()) {
            overlayAnimator.setAlpha(overlay, alphaFromCollapse);
        }
    }

    private boolean playingAndNotShowingAd() {
        return playSessionIsActive && !adOverlayShown;
    }

    @Override
    public void scrubStateChanged(int newScrubState) {
        isScrubbing = newScrubState == ScrubController.SCRUB_STATE_SCRUBBING;

        if (isScrubbing) {
            overlayAnimator.showOverlay(overlay);

        } else if (playingAndNotShowingAd() && isExpanded()) {
            overlayAnimator.hideOverlay(overlay);
        }
    }

    @Override
    public void displayScrubPosition(float scrubPosition) {
        // no-op
    }

    private boolean isExpanded() {
        return alphaFromCollapse == 0;
    }

    private boolean notScrubbing() {
        return !isScrubbing;
    }

    public static class Factory {
        private final Provider<OverlayAnimator> overlayAnimatorProvider;

        @Inject
        Factory(Provider<OverlayAnimator> overlayAnimatorProvider) {
            this.overlayAnimatorProvider = overlayAnimatorProvider;
        }

        public PlayerOverlayController create(View overlay) {
            return new PlayerOverlayController(overlay, overlayAnimatorProvider.get());
        }
    }
}
