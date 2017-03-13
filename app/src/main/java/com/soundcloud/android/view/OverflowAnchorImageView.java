package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * An unfortunately necessary class (for now). The problem: We want the Overflow Menus to overlap the anchors. To do
 * that, we must use the AppCompat version of PopupMenu. However, the AppCompat PopupMenu has a bug: If the anchor
 * is near the edge of the screen, it will immediately scroll the anchor view to the center of the screen, an odd user
 * experience to say the least.
 *
 * In order to prevent this, we use a custom image view and overwrite `#requestRectangleOnScreen` to always return
 * false.
 *
 * The bug and this workaround are documented on the Android Support Library Bug Tracker:
 * https://code.google.com/p/android/issues/detail?id=135439
 */
public class OverflowAnchorImageView extends AppCompatImageView {
    public OverflowAnchorImageView(Context context) {
        super(context);
    }

    public OverflowAnchorImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OverflowAnchorImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean requestRectangleOnScreen(Rect rectangle) {
        return false;
    }

    @Override
    public boolean requestRectangleOnScreen(Rect rectangle, boolean immediate) {
        return false;
    }
}
