package com.soundcloud.android.playback.ui.progress;

import static com.soundcloud.android.view.WaveformScrollView.OnScrollListener;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.view.WaveformScrollView;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.os.Message;

public class ScrubControllerTest extends AndroidUnitTest {

    private ScrubController scrubController;

    @Mock private WaveformScrollView scrollView;
    @Mock private PlaySessionController playSessionController;
    @Mock private ProgressHelper progressHelper;
    @Mock private ScrubController.OnScrubListener scrubListener;
    @Mock private SeekHandler.Factory seekHandlerFactory;
    @Mock private SeekHandler seekHandler;

    private Message message;
    private OnScrollListener scrollListener;

    @Before
    public void setUp() throws Exception {
        ArgumentCaptor<OnScrollListener> scrollListenerArgumentCaptor = ArgumentCaptor.forClass(OnScrollListener.class);

        when(seekHandlerFactory.create(any(ScrubController.class))).thenReturn(seekHandler);

        scrubController = new ScrubController(scrollView, playSessionController, seekHandlerFactory);
        scrubController.setFullDuration(100);
        scrubController.setProgressHelper(progressHelper);
        scrubController.addScrubListener(scrubListener);

        verify(scrollView).setListener(scrollListenerArgumentCaptor.capture());

        scrollListener = scrollListenerArgumentCaptor.getValue();

        message = Message.obtain();
    }

    @Test
    public void onActionDownEntersScrubbingMode() {
        scrollListener.onPress();
        verify(scrubListener).scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);
    }

    @Test
    public void onScrollCallsListenerWithNewScrubPositionIfDragging() {
        scrollListener.onPress();
        when(progressHelper.getProgressFromPosition(5)).thenReturn(.5f);
        scrollListener.onScroll(5,10);
        verify(scrubListener).displayScrubPosition(.5f, .5f);
    }

    @Test
    public void onScrollUpdatesListenerWithProportionGreaterThanOne() {
        scrollListener.onPress();
        when(progressHelper.getProgressFromPosition(11)).thenReturn(1.1f);
        scrollListener.onScroll(11,10);
        verify(scrubListener).displayScrubPosition(1.1f, 1.0f);
    }

    @Test
    public void onScrollUpdatesListenerWithProportionLessThanZero() {
        scrollListener.onPress();
        when(progressHelper.getProgressFromPosition(-1)).thenReturn(-0.1f);
        scrollListener.onScroll(-1,10);
        verify(scrubListener).displayScrubPosition(-.1f, 0f);
    }

    @Test
    public void onScrollRemovesExistingSeekMessagesIfDragging() {
        scrollListener.onPress();
        when(progressHelper.getProgressFromPosition(5)).thenReturn(.5f);
        scrollListener.onScroll(5,10);
        verify(seekHandler).removeMessages(ScrubController.MSG_PERFORM_SEEK);
    }

    @Test
    public void onScrollSendsSeekMessageIfDragging() {
        scrollListener.onPress();
        when(progressHelper.getProgressFromPosition(5)).thenReturn(.5f);
        when(seekHandler.obtainMessage(ScrubController.MSG_PERFORM_SEEK, .5f)).thenReturn(message);
        scrollListener.onScroll(5,10);
        verify(seekHandler).sendMessageDelayed(message, ScrubController.SEEK_DELAY);
    }

    @Test
    public void onScrollCallsListenerWithNewPositionIfMessageInQueue() {
        scrollListener.onPress();
        when(seekHandler.hasMessages(ScrubController.MSG_PERFORM_SEEK)).thenReturn(true);

        when(progressHelper.getProgressFromPosition(5)).thenReturn(.5f);
        scrollListener.onScroll(5,10);
        verify(scrubListener).displayScrubPosition(.5f, .5f);
    }

    @Test
    public void onScrollSendsSeekMessageIfMessageInQueue() {
        scrollListener.onPress();
        when(seekHandler.hasMessages(ScrubController.MSG_PERFORM_SEEK)).thenReturn(true);

        when(progressHelper.getProgressFromPosition(5)).thenReturn(.5f);
        when(seekHandler.obtainMessage(ScrubController.MSG_PERFORM_SEEK, .5f)).thenReturn(message);
        scrollListener.onScroll(5,10);
        verify(seekHandler).sendMessageDelayed(message, ScrubController.SEEK_DELAY);
    }

    @Test
    public void onScrollRemovesExistinSeekMessageIfMessageInQueue() {
        scrollListener.onPress();
        when(seekHandler.hasMessages(ScrubController.MSG_PERFORM_SEEK)).thenReturn(true);

        when(progressHelper.getProgressFromPosition(5)).thenReturn(.5f);
        scrollListener.onScroll(5, 10);
        verify(seekHandler).removeMessages(ScrubController.MSG_PERFORM_SEEK);
    }

    @Test
    public void onReleaseSetsScrubStateToNoneWithPendingSeek() {
        scrubController.setPendingSeek(.3f);
        scrollListener.onRelease();
        verify(scrubListener).scrubStateChanged(ScrubController.SCRUB_STATE_NONE);
    }

    @Test
    public void onReleaseSeeksWithPendingSeek() {
        scrubController.setPendingSeek(.3f);
        scrollListener.onRelease();
        verify(playSessionController).seek(30);
    }

    @Test
    public void onReleaseChangesStateToCancelledWithNoSeekMessages() {
        scrollListener.onRelease();
        verify(scrubListener).scrubStateChanged(ScrubController.SCRUB_STATE_CANCELLED);
    }

    @Test
    public void onReleaseDoesNotChangeStateWithSeekMessages() {
        scrubController.setPendingSeek(.3f);
        when(seekHandler.hasMessages(ScrubController.MSG_PERFORM_SEEK)).thenReturn(true);

        scrollListener.onRelease();
        verify(scrubListener, never()).scrubStateChanged(ScrubController.SCRUB_STATE_NONE);
    }

    @Test
    public void onReleaseDoesNotSeekWithSeekMessages() {
        scrubController.setPendingSeek(.3f);
        when(seekHandler.hasMessages(ScrubController.MSG_PERFORM_SEEK)).thenReturn(true);

        scrollListener.onRelease();
        verify(playSessionController, never()).seek(anyLong());
    }

    @Test
    public void onReleaseDoesNotSeekWithNoPendingSeek() {
        scrollListener.onRelease();
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
