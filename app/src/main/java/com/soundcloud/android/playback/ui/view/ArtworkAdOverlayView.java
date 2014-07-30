package com.soundcloud.android.playback.ui.view;

import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;


/**
 * ArtworkView that does not propogate clicks after scrolls. We only want this clickable on a tap.
 * This class is due to the disabled viewpager {@link com.soundcloud.android.playback.ui.view.PlayerTrackPager}
 * that would otherwise intercept the click.
 */
public class ArtworkAdOverlayView extends View {

    private final GestureDetectorCompat gestureDetectorCompat;

    public ArtworkAdOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        gestureDetectorCompat = new GestureDetectorCompat(context, new TapToClickListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetectorCompat.onTouchEvent(event);
        return true;
    }


    private final class TapToClickListener implements GestureDetector.OnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            performClick();
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            // no-op
        }

        @Override
        public void onLongPress(MotionEvent e) {
            // no-op
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    }
}
