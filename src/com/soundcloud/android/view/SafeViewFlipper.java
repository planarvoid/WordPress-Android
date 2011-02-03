
package com.soundcloud.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ViewFlipper;

// Workaround for http://code.google.com/p/android/issues/detail?id=6191
public class SafeViewFlipper extends ViewFlipper {
    public SafeViewFlipper(Context context) {
        super(context);
    }

    public SafeViewFlipper(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDetachedFromWindow() {
        try {
            super.onDetachedFromWindow();
        } catch (IllegalArgumentException e) {
            // Call stopFlipping() in order to kick off updateRunning()
            stopFlipping();
        }
    }
}
