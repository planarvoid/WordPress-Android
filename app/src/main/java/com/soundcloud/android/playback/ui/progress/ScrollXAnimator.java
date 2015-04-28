package com.soundcloud.android.playback.ui.progress;

import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class ScrollXAnimator extends ProgressAnimator {

    public ScrollXAnimator(final View progressView, float startX, float endX) {
        super(progressView, startX, endX);
    }

    @Override
    protected ObjectAnimator createAnimator(float startX, float endX) {
        final ObjectAnimator scrollX = ObjectAnimator.ofInt(progressView, "scrollX", (int) startX, (int) endX);
        scrollX.setInterpolator(new LinearInterpolator());
        return scrollX;
    }

    @Override
    public float getDifferenceFromCurrentValue(float targetValue) {
        return (int) wrappedAnimator.getAnimatedValue() - targetValue;
    }
}
