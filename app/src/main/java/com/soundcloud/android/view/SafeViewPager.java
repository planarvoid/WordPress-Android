package com.soundcloud.android.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;

public class SafeViewPager extends ViewPager {

    public SafeViewPager(Context context) {
        super(context);
    }

    public SafeViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Includes fix for ViewPager IllegalArgumentException during touch events.
     * This is currently viewed as safe to swallow
     * https://code.google.com/p/android/issues/detail?id=64553
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        try {
            return super.onTouchEvent(ev);
        } catch (IllegalArgumentException ignored) {
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException ignored) {
        }
        return false;
    }


    /**
     * Prevent ViewPager from swallowing HorizontalScrollView events
     * http://stackoverflow.com/questions/6920137/android-viewpager-and-horizontalscrollview
     */
    @Override
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        return super.canScroll(v, checkV, dx, x, y) || (checkV && customCanScroll(v));
    }

    protected boolean customCanScroll(View v) {
        return v instanceof HorizontalScrollView;
    }
}
