package com.soundcloud.android.playback.ui.view;

import com.soundcloud.android.view.SafeViewPager;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class PlayerTrackPager extends SafeViewPager {

    private boolean isPagingEnabled;

    public PlayerTrackPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.isPagingEnabled = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.isPagingEnabled) {
            return super.onTouchEvent(event);
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.isPagingEnabled) {
            return super.onInterceptTouchEvent(event);
        }
        return false;
    }

    public void setPagingEnabled(boolean enabled) {
        this.isPagingEnabled = enabled;
    }
}
