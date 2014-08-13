package com.soundcloud.android.playback.ui.view;

import com.soundcloud.android.view.SafeViewPager;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class PlayerTrackPager extends SafeViewPager {

    private final GestureDetector gestureDetector;

    private boolean isPagingEnabled;
    private OnBlockedSwipeListener onBlockedSwipeListener;

    public interface OnBlockedSwipeListener {
        void onBlockedSwipe();
    }

    public PlayerTrackPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        isPagingEnabled = true;
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public void setOnBlockedSwipeListener(OnBlockedSwipeListener onBlockedSwipeListener) {
        this.onBlockedSwipeListener = onBlockedSwipeListener;
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
        return gestureDetector.onTouchEvent(event);
    }

    public void setPagingEnabled(boolean enabled) {
        isPagingEnabled = enabled;
    }

    class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            onBlockedSwipeListener.onBlockedSwipe();
            return true;
        }
    }
}
