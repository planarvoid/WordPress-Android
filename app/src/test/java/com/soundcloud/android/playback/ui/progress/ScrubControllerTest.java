package com.soundcloud.android.playback.ui.progress;

import static com.soundcloud.android.view.ListenableHorizontalScrollView.OnScrollListener;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.view.ListenableHorizontalScrollView;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.os.Message;
import android.view.MotionEvent;
import android.view.View;

public class ScrubControllerTest extends AndroidUnitTest {

    private ScrubController scrubController;

    @Mock private ListenableHorizontalScrollView scrollView;
    @Mock private PlaySessionController playSessionController;
    @Mock private ProgressHelper progressHelper;
    @Mock private ScrubController.OnScrubListener scrubListener;
    @Mock private SeekHandler.Factory seekHandlerFactory;
    @Mock private SeekHandler seekHandler;

    private Message message;
    private OnScrollListener scrollListener;
    private View.OnTouchListener touchListener;

    @Before
    public void setUp() throws Exception {
        ArgumentCaptor<OnScrollListener> scrollListenerArgumentCaptor = ArgumentCaptor.forClass(OnScrollListener.class);
        ArgumentCaptor<View.OnTouchListener> touchListenerArgumentCaptor = ArgumentCaptor.forClass(View.OnTouchListener.class);

        when(seekHandlerFactory.create(any(ScrubController.class))).thenReturn(seekHandler);

        scrubController = new ScrubController(scrollView, playSessionController, seekHandlerFactory);
        scrubController.setDuration(100);
        scrubController.setProgressHelper(progressHelper);
        scrubController.addScrubListener(scrubListener);

        verify(scrollView).setOnScrollListener(scrollListenerArgumentCaptor.capture());
        verify(scrollView).setOnTouchListener(touchListenerArgumentCaptor.capture());

        scrollListener = scrollListenerArgumentCaptor.getValue();
        touchListener = touchListenerArgumentCaptor.getValue();

        message = Message.obtain();
    }

    @Test
    public void onActionDownEntersScrubbingMode() {
        touchListener.onTouch(scrollView, MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN, 0,0,0));
        verify(scrubListener).scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);
    }

    @Test
    public void onScrollCallsListenerWithNewScrubPositionIfDragging() {
        touchListener.onTouch(scrollView, MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN, 0,0,0));
        when(progressHelper.getProgressFromPosition(5)).thenReturn(.5f);
        scrollListener.onScroll(5,10);
        verify(scrubListener).displayScrubPosition(.5f);
    }

    @Test
    public void onScrollCannotUpdateListenerWithProportionGreaterThanOne() {
        touchListener.onTouch(scrollView, MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN, 0,0,0));
        when(progressHelper.getProgressFromPosition(11)).thenReturn(1.1f);
        scrollListener.onScroll(11,10);
        verify(scrubListener).displayScrubPosition(1.0f);
    }

    @Test
    public void onScrollCannotUpdateListenerWithProportionLessThanZero() {
        touchListener.onTouch(scrollView, MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN, 0,0,0));
        when(progressHelper.getProgressFromPosition(-1)).thenReturn(-0.1f);
        scrollListener.onScroll(-1,10);
        verify(scrubListener).displayScrubPosition(0.0f);
    }

    @Test
    public void onScrollRemovesExistingSeekMessagesIfDragging() {
        touchListener.onTouch(scrollView, MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN, 0,0,0));
        when(progressHelper.getProgressFromPosition(5)).thenReturn(.5f);
        scrollListener.onScroll(5,10);
        verify(seekHandler).removeMessages(ScrubController.MSG_PERFORM_SEEK);
    }

    @Test
    public void onScrollSendsSeekMessageIfDragging() {
        touchListener.onTouch(scrollView, MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN, 0,0,0));
        when(progressHelper.getProgressFromPosition(5)).thenReturn(.5f);
        when(seekHandler.obtainMessage(ScrubController.MSG_PERFORM_SEEK, .5f)).thenReturn(message);
        scrollListener.onScroll(5,10);
        verify(seekHandler).sendMessageDelayed(message, ScrubController.SEEK_DELAY);
    }

    @Test
    public void onScrollCallsListenerWithNewPositionIfMessageInQueue() {
        touchListener.onTouch(scrollView, MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN, 0,0,0));
        when(seekHandler.hasMessages(ScrubController.MSG_PERFORM_SEEK)).thenReturn(true);

        when(progressHelper.getProgressFromPosition(5)).thenReturn(.5f);
        scrollListener.onScroll(5,10);
        verify(scrubListener).displayScrubPosition(.5f);
    }

    @Test
    public void onScrollSendsSeekMessageIfMessageInQueue() {
        touchListener.onTouch(scrollView, MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN, 0,0,0));
        when(seekHandler.hasMessages(ScrubController.MSG_PERFORM_SEEK)).thenReturn(true);

        when(progressHelper.getProgressFromPosition(5)).thenReturn(.5f);
        when(seekHandler.obtainMessage(ScrubController.MSG_PERFORM_SEEK, .5f)).thenReturn(message);
        scrollListener.onScroll(5,10);
        verify(seekHandler).sendMessageDelayed(message, ScrubController.SEEK_DELAY);
    }

    @Test
    public void onScrollRemovesExistinSeekMessageIfMessageInQueue() {
        touchListener.onTouch(scrollView, MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN, 0,0,0));
        when(seekHandler.hasMessages(ScrubController.MSG_PERFORM_SEEK)).thenReturn(true);

        when(progressHelper.getProgressFromPosition(5)).thenReturn(.5f);
        scrollListener.onScroll(5,10);
        verify(seekHandler).removeMessages(ScrubController.MSG_PERFORM_SEEK);
    }

    @Test
    public void onActionUpSetsScrubStateToNoneWithPendingSeek() {
        scrubController.setPendingSeek(.3f);
        touchListener.onTouch(scrollView, MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 0, 0, 0));
        verify(scrubListener).scrubStateChanged(ScrubController.SCRUB_STATE_NONE);
    }

    @Test
    public void onActionUpSeeksWithPendingSeek() {
        scrubController.setPendingSeek(.3f);
        touchListener.onTouch(scrollView, MotionEvent.obtain(0,0,MotionEvent.ACTION_UP, 0,0,0));
        verify(playSessionController).seek(30);
    }

    @Test
    public void onActionUpChangesStateToCancelledWithNoSeekMessages() {
        touchListener.onTouch(scrollView, MotionEvent.obtain(0,0,MotionEvent.ACTION_UP, 0,0,0));
        verify(scrubListener).scrubStateChanged(ScrubController.SCRUB_STATE_CANCELLED);
    }

    @Test
    public void motionEventChangesStateToCancelledIfOutsideViewBounds() {
        when(scrollView.getLeft()).thenReturn(0);
        when(scrollView.getRight()).thenReturn(20);
        when(scrollView.getTop()).thenReturn(0);
        when(scrollView.getBottom()).thenReturn(20);

        touchListener.onTouch(scrollView, MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, 50, 50, 0));
        verify(scrubListener).scrubStateChanged(ScrubController.SCRUB_STATE_CANCELLED);
    }

    @Test
    public void onActionUpDoesNotChangeStateWithSeekMessages() {
        scrubController.setPendingSeek(.3f);
        when(seekHandler.hasMessages(ScrubController.MSG_PERFORM_SEEK)).thenReturn(true);

        touchListener.onTouch(scrollView, MotionEvent.obtain(0,0,MotionEvent.ACTION_UP, 0,0,0));
        verify(scrubListener, never()).scrubStateChanged(ScrubController.SCRUB_STATE_NONE);
    }

    @Test
    public void onActionUpDoesNotSeekWithSeekMessages() {
        scrubController.setPendingSeek(.3f);
        when(seekHandler.hasMessages(ScrubController.MSG_PERFORM_SEEK)).thenReturn(true);

        touchListener.onTouch(scrollView, MotionEvent.obtain(0,0,MotionEvent.ACTION_UP, 0,0,0));
        verify(playSessionController, never()).seek(anyLong());
    }

    @Test
    public void onActionUpDoesNotSeekWithNoPendingSeek() {
        touchListener.onTouch(scrollView, MotionEvent.obtain(0,0,MotionEvent.ACTION_UP, 0,0,0));
        verify(playSessionController, never()).seek(anyLong());
    }

    @Test
    public void finshSeekSeeks() {
        scrubController.finishSeek(.3F);
        verify(playSessionController).seek(30);
    }

    @Test
    public void finshSeekSetsScrubStateToNone() {
        scrubController.finishSeek(.3F);
        verify(scrubListener).scrubStateChanged(ScrubController.SCRUB_STATE_NONE);
    }
}