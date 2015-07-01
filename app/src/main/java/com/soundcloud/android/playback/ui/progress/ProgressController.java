package com.soundcloud.android.playback.ui.progress;

import com.soundcloud.android.playback.PlaybackProgress;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

import javax.inject.Inject;

public class ProgressController {

    private static final int PROGRESS_SYNC_TOLERANCE = 1; // allow animation to stray 1 pixel before correcting it

    private PlaybackProgress playbackProgress;
    private ProgressAnimator progressAnimator;

    private View progressView;
    private ProgressHelper helper;
    private boolean animationRequested;
    private final AnimatorListenerAdapter resetProgressListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            progressAnimator.removeListener(this);
            helper.setValueFromProportion(progressView, 0);
        }
    };

    public ProgressController(View progressView) {
        this(progressView, new EmptyProgressHelper());
    }

    public ProgressController(View progressView, ProgressHelper helper) {
        this.progressView = progressView;
        this.helper = helper;
    }

    public void setHelper(ProgressHelper helper) {
        this.helper = helper;

        if (hasRunningAnimation() || animationRequested) {
            startProgressAnimationInternal();
        }
    }

    public void reset() {
        animationRequested = false;
        if (progressAnimator != null && progressAnimator.isRunning()) {
            progressAnimator.addListener(resetProgressListener);
            progressAnimator.cancel();
        } else {
            setPlaybackProgress(PlaybackProgress.empty());
        }
    }

    private boolean hasRunningAnimation() {
        return progressAnimator != null && progressAnimator.isRunning();
    }

    public void startProgressAnimation(PlaybackProgress playbackProgress) {
        animationRequested = true;
        this.playbackProgress = playbackProgress;
        startProgressAnimationInternal();
    }

    public void cancelProgressAnimation() {
        animationRequested = false;
        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
    }

    public void setPlaybackProgress(PlaybackProgress playbackProgress) {
        this.playbackProgress = playbackProgress;
        final float progressProportion = playbackProgress.getProgressProportion();
        if (hasRunningAnimation()) {
            final float expectedValue = helper.getValueFromProportion(progressProportion);
            if (progressAnimator.getDifferenceFromCurrentValue(expectedValue) > PROGRESS_SYNC_TOLERANCE) {
                startProgressAnimationInternal();
            }
        } else {
            helper.setValueFromProportion(progressView, progressProportion);
        }
    }

    private void startProgressAnimationInternal() {
        if (progressAnimator != null){
            progressAnimator.cancel();
        }
        progressAnimator = helper.createAnimator(progressView, playbackProgress.getProgressProportion());
        if (progressAnimator != null) {
            progressAnimator.setDuration(playbackProgress.getTimeLeft());
            progressAnimator.start();
            // this needs to happen after start, even though the documentation leads me to believe otherwise : JS
            progressAnimator.setCurrentPlayTime(playbackProgress.getTimeSinceCreation());
        }
    }

    public static class Factory {

        @Inject
        public Factory() {
            // Required by Dagger
        }

        public ProgressController create(View progressView) {
            return new ProgressController(progressView);
        }
    }

}
