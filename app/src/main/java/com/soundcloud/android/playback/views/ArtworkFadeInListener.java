package com.soundcloud.android.playback.views;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.utils.AnimUtils;

import android.view.animation.Animation;

import java.lang.ref.WeakReference;

@VisibleForTesting
class ArtworkFadeInListener extends AnimUtils.SimpleAnimationListener {
    private WeakReference<ArtworkTrackView> mTrackViewRef;

    ArtworkFadeInListener(ArtworkTrackView trackView) {
        this.mTrackViewRef = new WeakReference<ArtworkTrackView>(trackView);
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        ArtworkTrackView trackView = mTrackViewRef.get();
        if (trackView != null) {
            trackView.clearBackgroundAfterAnimation(animation);
        }
    }
}
