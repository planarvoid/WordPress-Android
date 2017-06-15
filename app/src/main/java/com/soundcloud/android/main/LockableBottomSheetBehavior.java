package com.soundcloud.android.main;

import android.content.Context;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import javax.inject.Inject;

public class LockableBottomSheetBehavior<V extends View> extends BottomSheetBehavior<V> {

    private boolean locked;

    public LockableBottomSheetBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LockableBottomSheetBehavior() {
        super();
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        return !locked && super.onInterceptTouchEvent(parent, child, event);
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public static class Factory {

        @Inject
        public Factory() {
        }

        @SuppressWarnings("unchecked")
        public <V extends View> LockableBottomSheetBehavior<V> from(V view) {
            BottomSheetBehavior behavior = BottomSheetBehavior.from(view);
            if (behavior instanceof LockableBottomSheetBehavior) {
                return (LockableBottomSheetBehavior) behavior;
            } else {
                throw new IllegalArgumentException("The view is not associated with LockableBottomSheetBehavior");
            }
        }
    }
}
