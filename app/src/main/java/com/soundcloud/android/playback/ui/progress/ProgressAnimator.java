package com.soundcloud.android.playback.ui.progress;

import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ObjectAnimator;

import android.view.View;

public abstract class ProgressAnimator {
    private final View progressView;
    protected final ObjectAnimator wrappedAnimator;

    public ProgressAnimator(final View progressView, float startX, float endX) {
        wrappedAnimator = createAnimator(progressView, startX, endX);
        this.progressView = progressView;
    }

    protected abstract ObjectAnimator createAnimator(View progressView, float startX, float endX);

    public abstract float getDifferenceFromCurrentValue(float targetValue);

    public void start() {
        wrappedAnimator.start();
    }

    public void setDuration(long duration) {
        wrappedAnimator.setDuration(duration);
    }

    public void setCurrentPlayTime(long currentPlayTime) {
        wrappedAnimator.setCurrentPlayTime(currentPlayTime);
    }

    public void cancel() {
        wrappedAnimator.cancel();
        progressView.clearAnimation();
    }

    public boolean isRunning() {
        return wrappedAnimator.isRunning();
    }

    protected void addListener(AnimatorListenerAdapter listenerAdapter) {
        wrappedAnimator.addListener(listenerAdapter);
    }
}
