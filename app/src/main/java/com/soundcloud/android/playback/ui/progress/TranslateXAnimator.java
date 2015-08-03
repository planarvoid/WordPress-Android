package com.soundcloud.android.playback.ui.progress;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class TranslateXAnimator extends ProgressAnimator {

    public TranslateXAnimator(final View progressView, float startX, float endX) {
        super(progressView, startX, endX);
    }

    @Override
    protected ObjectAnimator createAnimator(float startX, float endX) {
        final ObjectAnimator translationX = ObjectAnimator.ofFloat(progressView, "translationX", startX, endX);
        translationX.setInterpolator(new LinearInterpolator());
        return translationX;
    }

    @Override
    public float getDifferenceFromCurrentValue(float targetValue) {
        return (Float) wrappedAnimator.getAnimatedValue() - targetValue;
    }
}
