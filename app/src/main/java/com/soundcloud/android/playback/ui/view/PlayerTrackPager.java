package com.soundcloud.android.playback.ui.view;

import com.soundcloud.android.view.SafeViewPager;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class PlayerTrackPager extends SafeViewPager {

    private boolean isPagingEnabled;

    public PlayerTrackPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        isPagingEnabled = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isPagingEnabled) {
            return super.onTouchEvent(event);
        }
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (isPagingEnabled) {
            return super.onInterceptTouchEvent(event);
        }
        return true;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        // we manually manage all state restoration. Allowing Android to do this actually causes bugs
        // like being on the wrong page after coming back from the background (annotate this to find out specifically)
        super.onRestoreInstanceState(null);
    }

    public void setPagingEnabled(boolean enabled) {
        isPagingEnabled = enabled;
    }
}
