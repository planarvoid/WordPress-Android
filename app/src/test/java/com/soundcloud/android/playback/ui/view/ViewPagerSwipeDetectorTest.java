package com.soundcloud.android.playback.ui.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;

import android.view.MotionEvent;

import java.util.concurrent.TimeUnit;

public class ViewPagerSwipeDetectorTest extends AndroidUnitTest {

    private final TestDateProvider dateProvider = new TestDateProvider();
    private  StubbedSwipeListener listener = new StubbedSwipeListener();

    private ViewPagerSwipeDetector detector = new ViewPagerSwipeDetector(1, TimeUnit.SECONDS, dateProvider);

    @Before
    public void setUp() throws Exception {
        listener = new StubbedSwipeListener();
        detector.setListener(listener);
    }

    @Test
    public void notifiesWhenSequenceMatches() {
        detector.onTouchEvent(motion(MotionEvent.ACTION_UP));
        detector.onPageSelected(0);

        assertThat(listener.hasSwiped).isTrue();
    }

    @Test
    public void doesNotNotifyWhenPageNotSelected() {
        detector.onTouchEvent(motion(MotionEvent.ACTION_UP));

        assertThat(listener.hasSwiped).isFalse();
    }

    @Test
    public void doesNotNotifyWhenNoActionReceived() {
        detector.onPageSelected(0);

        assertThat(listener.hasSwiped).isFalse();
    }

    @Test
    public void doesNotNotifyWhenPageActionUpNotReceived() {
        detector.onTouchEvent(motion(MotionEvent.ACTION_DOWN));
        detector.onPageSelected(0);

        assertThat(listener.hasSwiped).isFalse();
    }

    @Test
    public void doesNotNotifyWhenPageMotionUpReceivedAfterPageSelected() {
        detector.onPageSelected(0);
        detector.onTouchEvent(motion(MotionEvent.ACTION_UP));

        assertThat(listener.hasSwiped).isFalse();
    }

    @Test
    public void doesNotNotifyWhenEventTimedOut() {
        detector.onTouchEvent(motion(MotionEvent.ACTION_UP));
        dateProvider.advanceBy(2, TimeUnit.SECONDS);

        detector.onPageSelected(0);

        assertThat(listener.hasSwiped).isFalse();
    }

    private MotionEvent motion(int action) {
        return MotionEvent.obtain(0, 0, action, 0, 0, 0);
    }

    private static class StubbedSwipeListener implements ViewPagerSwipeDetector.SwipeListener {
        public boolean hasSwiped;

        @Override
        public void onSwipe() {
            hasSwiped = true;
        }
    }
}
