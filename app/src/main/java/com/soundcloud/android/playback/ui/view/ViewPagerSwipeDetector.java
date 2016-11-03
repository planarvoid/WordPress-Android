package com.soundcloud.android.playback.ui.view;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;

import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewPager;
import android.view.MotionEvent;

import java.util.concurrent.TimeUnit;

/*
 * The ViewPager detects swipe gestures and selects accordingly the current page.
 *
 * Unfortunately, `OnPageChangeListener` does not tell when on page was selected from a swipe or a direct
 * call to `setCurrentItem`.
 *
 * `PagerSwipeDetector` tries to bridge the gap.
 */
public class ViewPagerSwipeDetector extends ViewPager.SimpleOnPageChangeListener {
    public static final SwipeListener EMPTY_LISTENER = new SwipeListener() {
        @Override
        public void onSwipe() {
            // no-op
        }
    };

    public static ViewPagerSwipeDetector forPager(ViewPager pager) {
        final CurrentDateProvider dateProvider = new CurrentDateProvider();
        final ViewPagerSwipeDetector detector = new ViewPagerSwipeDetector(500, TimeUnit.MILLISECONDS, dateProvider);
        pager.addOnPageChangeListener(detector);

        return detector;
    }

    private static final int NOT_SET = 0;

    public interface SwipeListener {
        void onSwipe();
    }

    private final DateProvider dateProvider;
    private final long threshold;

    private SwipeListener listener = EMPTY_LISTENER;
    private long gestureFinishedTime;

    ViewPagerSwipeDetector(int value, TimeUnit unit, DateProvider dateProvider) {
        this.dateProvider = dateProvider;
        this.threshold = unit.toMillis(value);
        reset();
    }

    public void onTouchEvent(MotionEvent event) {
        if (isGestureFinished(event)) {
            onGestureDone();
        } else {
            reset();
        }
    }

    public void setListener(SwipeListener skipListener) {
        this.listener = checkNotNull(skipListener);
    }

    private boolean isGestureFinished(MotionEvent event) {
        final int action = event.getAction() & MotionEventCompat.ACTION_MASK;
        return action == MotionEvent.ACTION_UP;
    }

    @Override
    public void onPageSelected(int position) {
        if (matchSwipeGesture()) {
            reset();
            listener.onSwipe();
        }
    }

    private void reset() {
        this.gestureFinishedTime = NOT_SET;
    }

    private void onGestureDone() {
        this.gestureFinishedTime = dateProvider.getCurrentTime();
    }

    private boolean matchSwipeGesture() {
        final long delay = dateProvider.getCurrentTime() - gestureFinishedTime;
        return delay <= threshold;
    }
}
