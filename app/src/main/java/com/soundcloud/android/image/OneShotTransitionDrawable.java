package com.soundcloud.android.image;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;

public class OneShotTransitionDrawable extends TransitionDrawable {

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
}
