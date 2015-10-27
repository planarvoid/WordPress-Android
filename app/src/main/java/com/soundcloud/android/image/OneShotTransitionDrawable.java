package com.soundcloud.android.image;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;

public class OneShotTransitionDrawable extends TransitionDrawable {

    // This is required because of CircleImageView library bug that requires width and height
    // for everything that is not a ColorDrawable
    private final static int DIMENSION = 2;

    private boolean hasStarted;

    public OneShotTransitionDrawable(Drawable[] layers) {
        super(layers);
    }

    @Override
    public void startTransition(int durationMillis) {
        if (!hasStarted) {
            hasStarted = true;
            super.startTransition(durationMillis);
        }
    }

    @Override
    public int getIntrinsicWidth() {
        return DIMENSION;
    }

    @Override
    public int getIntrinsicHeight() {
        return DIMENSION;
    }
}
