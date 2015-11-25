package com.soundcloud.android.view;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.MotionEvent;

public class WaveformScrollViewTest extends AndroidUnitTest {

    private WaveformScrollView waveformScrollView;

    @Mock private WaveformScrollView.OnScrollListener listener;

    @Before
    public void setUp() throws Exception {
        waveformScrollView = new WaveformScrollView(context());
        waveformScrollView.setListener(listener);
    }

    @Test
    public void callsOnScroll() {
        waveformScrollView.onScrollChanged(10, 0, 5, 0);

        verify(listener).onScroll(10, 5);
    }

    @Test
    public void callsOnPressWithDownAction() {
        waveformScrollView.onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50, 50, 0));

        verify(listener).onPress();
    }

    @Test
    public void callsOnReleaseWithUpAction() {
        waveformScrollView.onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 50, 50, 0));

        verify(listener).onRelease();
    }

    @Test
    public void callsOnReleaseWithCancelAction() {
        waveformScrollView.onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 50, 50, 0));

        verify(listener).onRelease();
    }

    @Test
    public void callsOnReleaseWithActionOutsideBounds() {
        waveformScrollView.onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, -50, -50, 0));

        verify(listener).onRelease();
    }
}
