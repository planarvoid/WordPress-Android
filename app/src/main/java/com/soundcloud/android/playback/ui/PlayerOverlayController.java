package com.soundcloud.android.playback.ui;

import com.soundcloud.android.ads.LeaveBehindController;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.ui.progress.ScrubController;

import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;

class PlayerOverlayController implements ScrubController.OnScrubListener {

    private final OverlayAnimator overlayAnimator;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final LeaveBehindController leaveBehindController;
    private final View overlay;
    private boolean isScrubbing;
    private float alphaFromCollapse;

    public PlayerOverlayController(View overlay,
                                   OverlayAnimator overlayAnimator,
                                   PlaySessionStateProvider playSessionStateProvider,
                                   LeaveBehindController leaveBehindController) {
        this.overlay = overlay;
        this.overlayAnimator = overlayAnimator;
        this.playSessionStateProvider = playSessionStateProvider;
        this.leaveBehindController = leaveBehindController;
    }

    public void showPlayingState(){
        if (notScrubbing() && isExpanded() && isLeaveBehindDisabled()){
            overlayAnimator.hideOverlay(overlay);
        }
    }

    public void showIdleState() {
        overlayAnimator.showOverlay(overlay);
    }

    public void setAlphaFromCollapse(float alpha) {
        alphaFromCollapse = alpha;

        if (playSessionStateProvider.isPlaying() && isLeaveBehindDisabled()) {
            overlayAnimator.setAlpha(overlay, alphaFromCollapse);
        }
    }

    @Override
    public void scrubStateChanged(int newScrubState) {
        isScrubbing = newScrubState == ScrubController.SCRUB_STATE_SCRUBBING;

        if (isScrubbing){
            overlayAnimator.showOverlay(overlay);

        } else if (playSessionStateProvider.isPlaying() && isExpanded() && isLeaveBehindDisabled()){
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

    private boolean isLeaveBehindDisabled() {
        return leaveBehindController == null || leaveBehindController.isNotVisible();
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
            return new PlayerOverlayController(overlay, overlayAnimatorProvider.get(), playSessionStateProvider, null);
        }

        public PlayerOverlayController create(View overlay, LeaveBehindController leaveBehindController) {
            return new PlayerOverlayController(overlay, overlayAnimatorProvider.get(), playSessionStateProvider, leaveBehindController);
        }

    }
}
