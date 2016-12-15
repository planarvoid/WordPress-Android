package com.soundcloud.android.cast;

import android.animation.ValueAnimator;
import android.view.View;

public class RedrawLayoutListener implements ValueAnimator.AnimatorUpdateListener {

    private final View layout;

    public RedrawLayoutListener(View layout) {
        this.layout = layout;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator valueAnimator) {
        layout.getLayoutParams().height = (int) valueAnimator.getAnimatedValue();
        layout.requestLayout();
    }
}
