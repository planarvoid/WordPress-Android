package com.soundcloud.android.playback.ui.progress;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.support.annotation.VisibleForTesting;
import android.view.View;

import java.util.List;

public abstract class ProgressAnimator {

    protected final View progressView;
    protected final ObjectAnimator wrappedAnimator;

    public ProgressAnimator(final View progressView, float startX, float endX) {
        this.progressView = progressView;
        wrappedAnimator = createAnimator(startX, endX);
    }

    protected abstract ObjectAnimator createAnimator(float startX, float endX);

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
    }

    public boolean isRunning() {
        return wrappedAnimator.isRunning();
    }

    protected void addListener(AnimatorListenerAdapter listenerAdapter) {
        wrappedAnimator.addListener(listenerAdapter);
    }

    protected void removeListener(AnimatorListenerAdapter listenerAdapter) {
        wrappedAnimator.removeListener(listenerAdapter);
    }
    
    @VisibleForTesting
    List<Animator.AnimatorListener> getWrappedAnimationListeners(){
        return wrappedAnimator.getListeners();
    }

}
