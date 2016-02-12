package com.soundcloud.android.transition;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.annotation.TargetApi;
import android.os.Build;
import android.util.ArrayMap;

import java.util.ArrayList;

/**
 * Workaround:
 * Interrupting Activity transitions can yield an OperationNotSupportedException when the
 * transition tries to pause the animator. This wraps the Animator to ignore pause/resume.
 * https://halfthought.wordpress.com/2014/11/07/reveal-transition
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class NoPauseAnimator extends Animator {

    private final Animator animator;
    private final ArrayMap<AnimatorListener, AnimatorListener> listeners =
            new ArrayMap<>();

    public NoPauseAnimator(Animator animator) {
        this.animator = animator;
    }

    @Override
    public void addListener(AnimatorListener listener) {
        AnimatorListener wrapper = new AnimatorListenerWrapper(this, listener);
        if (!listeners.containsKey(listener)) {
            listeners.put(listener, wrapper);
            animator.addListener(wrapper);
        }
    }

    @Override
    public void cancel() {
        animator.cancel();
    }

    @Override
    public void end() {
        animator.end();
    }

    @Override
    public long getDuration() {
        return animator.getDuration();
    }

    @Override
    public TimeInterpolator getInterpolator() {
        return animator.getInterpolator();
    }

    @Override
    public void setInterpolator(TimeInterpolator timeInterpolator) {
        animator.setInterpolator(timeInterpolator);
    }

    @Override
    public ArrayList<AnimatorListener> getListeners() {
        return new ArrayList<>(listeners.keySet());
    }

    @Override
    public long getStartDelay() {
        return animator.getStartDelay();
    }

    @Override
    public void setStartDelay(long delayMS) {
        animator.setStartDelay(delayMS);
    }

    @Override
    public boolean isPaused() {
        return animator.isPaused();
    }

    @Override
    public boolean isRunning() {
        return animator.isRunning();
    }

    @Override
    public boolean isStarted() {
        return animator.isStarted();
    }

    @Override
    public void removeAllListeners() {
        listeners.clear();
        animator.removeAllListeners();
    }

    @Override
    public void removeListener(AnimatorListener listener) {
        AnimatorListener wrapper = listeners.get(listener);
        if (wrapper != null) {
            listeners.remove(listener);
            animator.removeListener(wrapper);
        }
    }

    @Override
    public Animator setDuration(long durationMS) {
        animator.setDuration(durationMS);
        return this;
    }

    @Override
    public void setTarget(Object target) {
        animator.setTarget(target);
    }

    @Override
    public void setupEndValues() {
        animator.setupEndValues();
    }

    @Override
    public void setupStartValues() {
        animator.setupStartValues();
    }

    @Override
    public void start() {
        animator.start();
    }

    static class AnimatorListenerWrapper implements Animator.AnimatorListener {
        private final Animator mAnimator;
        private final Animator.AnimatorListener mListener;

        public AnimatorListenerWrapper(Animator animator, Animator.AnimatorListener listener) {
            mAnimator = animator;
            mListener = listener;
        }

        @Override
        public void onAnimationStart(Animator animator) {
            mListener.onAnimationStart(mAnimator);
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            mListener.onAnimationEnd(mAnimator);
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            mListener.onAnimationCancel(mAnimator);
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
            mListener.onAnimationRepeat(mAnimator);
        }
    }

}
