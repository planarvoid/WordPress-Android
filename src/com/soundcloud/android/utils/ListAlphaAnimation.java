package com.soundcloud.android.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class ListAlphaAnimation extends AlphaAnimation{
    private Animation mAnimation;
    private long mLastTime;

    public ListAlphaAnimation() {
        super(0.0f,1.0f);
        setDuration(500);
    }

    public long getLastAnimationTime(){
        Log.i("asdf","Returning " + mLastTime);
        return mLastTime;
    }

    @Override
    public boolean getTransformation(long currentTime, Transformation outTransformation) {
        mLastTime = currentTime;
        return super.getTransformation(currentTime,outTransformation);
    }

    @Override
    public void applyTransformation(float interpolatedTime, Transformation t) {
        t.setAlpha((float) (0 + ((1.0 - 0) * interpolatedTime)));
    }
}
