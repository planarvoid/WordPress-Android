package com.soundcloud.android.image;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;

public class OneShotTransitionDrawable extends TransitionDrawable {

    private boolean mStarted;

    public OneShotTransitionDrawable(Drawable[] layers) {
        super(layers);
    }

    @Override
    public void startTransition(int durationMillis){
        if (!mStarted){
            mStarted = true;
            super.startTransition(durationMillis);
        }
    }
}
