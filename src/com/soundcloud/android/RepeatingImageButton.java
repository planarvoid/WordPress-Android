package com.soundcloud.android;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

public class RepeatingImageButton extends ImageButton {
	private long mStartTime;
    private int mRepeatCount;
    private RepeatListener mListener;
    private long mInterval = 500;
    
    public RepeatingImageButton(Context context) {
        this(context, null);
    }

    public RepeatingImageButton(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.imageButtonStyle);
    }

    public RepeatingImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setFocusable(true);
        setLongClickable(true);
    }
    
    /**
     * Sets the listener to be called while the button is pressed and
     * the interval in milliseconds with which it will be called.
     * @param l The listener that will be called
     * @param interval The interval in milliseconds for calls 
     */
    public void setRepeatListener(RepeatListener l, long interval) {
        mListener = l;
        mInterval = interval;
    }
    
    @Override
    public boolean performLongClick() {
        mStartTime = SystemClock.elapsedRealtime();
        mRepeatCount = 0;
        post(mRepeater);
        return true;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            // remove the repeater, but call the hook one more time
            removeCallbacks(mRepeater);
            if (mStartTime != 0) {
                doRepeat(true);
                mStartTime = 0;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                // need to call super to make long press work, but return
                // true so that the application doesn't get the down event.
                super.onKeyDown(keyCode, event);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_ENTER:
            // remove the repeater, but call the hook one more time
            removeCallbacks(mRepeater);
            if (mStartTime != 0) {
                doRepeat(true);
                mStartTime = 0;
            }
        }
        return super.onKeyUp(keyCode, event);
    }
    
    private Runnable mRepeater = new Runnable() {
        public void run() {
            doRepeat(false);
            if (isPressed()) {
                postDelayed(this, mInterval);
            }
        }
    };

    private  void doRepeat(boolean last) {
        long now = SystemClock.elapsedRealtime();
        if (mListener != null) {
            mListener.onRepeat(this, now - mStartTime, last ? -1 : mRepeatCount++);
        }
    }
    
    public interface RepeatListener {
        /**
         * This method will be called repeatedly at roughly the interval
         * specified in setRepeatListener(), for as long as the button
         * is pressed.
         * @param v The button as a View.
         * @param duration The number of milliseconds the button has been pressed so far.
         * @param repeatcount The number of previous calls in this sequence.
         * If this is going to be the last call in this sequence (i.e. the user
         * just stopped pressing the button), the value will be -1.  
         */
        void onRepeat(View v, long duration, int repeatcount);
    }

}
