package com.soundcloud.android.view.play;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.utils.AnimUtils;

import android.view.animation.Animation;

import java.lang.ref.WeakReference;

@VisibleForTesting
class ArtworkFadeInListener extends AnimUtils.SimpleAnimationListener {
    private WeakReference<PlayerArtworkTrackView> mTrackViewRef;

    ArtworkFadeInListener(PlayerArtworkTrackView trackView) {
        this.mTrackViewRef = new WeakReference<PlayerArtworkTrackView>(trackView);
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        PlayerArtworkTrackView trackView = mTrackViewRef.get();
        if (trackView != null) {
            trackView.clearBackgroundAfterAnimation(animation);
        }
    }
}
