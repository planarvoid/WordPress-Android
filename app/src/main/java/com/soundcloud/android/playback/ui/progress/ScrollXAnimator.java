package com.soundcloud.android.playback.ui.progress;

import com.nineoldandroids.animation.ObjectAnimator;

import android.view.View;
import android.view.animation.LinearInterpolator;

public class ScrollXAnimator extends ProgressAnimator {

    public ScrollXAnimator(final View progressView, Integer startX, Integer endX) {
        super(progressView, startX, endX);
    }

    @Override
    protected ObjectAnimator createAnimator(View progressView, float startX, float endX) {
        final ObjectAnimator translationX = ObjectAnimator.ofInt(progressView, "scrollX", (int) startX, (int) endX);
        translationX.setInterpolator(new LinearInterpolator());
        return translationX;
    }

    @Override
    public float getDifferenceFromCurrentValue(float targetValue) {
        return (Integer) wrappedAnimator.getAnimatedValue() - targetValue;
    }
}
