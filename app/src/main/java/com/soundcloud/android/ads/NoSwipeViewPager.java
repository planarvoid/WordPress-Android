package com.soundcloud.android.ads;

import com.soundcloud.android.view.SafeViewPager;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class NoSwipeViewPager extends SafeViewPager {

    public NoSwipeViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return false;
    }
}
