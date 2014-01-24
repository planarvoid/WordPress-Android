package com.soundcloud.android.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class SafeViewPager extends ViewPager {

    public SafeViewPager(Context context) {
        super(context);
    }

    public SafeViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            // Swallow framework issue in ScaleGestureDetector
        }
        return false;
    }
}
