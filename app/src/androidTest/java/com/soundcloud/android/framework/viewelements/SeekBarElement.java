package com.soundcloud.android.framework.viewelements;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class SeekBarElement {
    protected final SeekBar view;

    public SeekBarElement(ViewElement element) {
        this.view = (SeekBar) element.getView();
    }

    public int getProgress() {
        return view.getProgress();
    }

    public void tapAt(final int percentage) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                int x = view.getMeasuredWidth() * percentage / 100;
                int y = view.getMeasuredHeight() / 2;

                MotionEvent event = MotionEvent.obtain(1000l, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0);
                view.dispatchTouchEvent(event);
            }
        });
    }
}
