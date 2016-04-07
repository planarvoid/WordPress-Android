package com.soundcloud.android.playback;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.times;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.schedulers.TestScheduler;

import java.util.concurrent.TimeUnit;

public class FadeHandlerTest extends AndroidUnitTest {
    private static final long STEP_MS = 10;
    private static final long HALF_SECOND = 500;
    private static final long ONE_SECOND = 1000;
    private static final long TWO_SECONDS = 2000;
    private static final long TWO_STEPS = 2 * STEP_MS;

    private static final FadeRequest FADE_OUT_ONE_SECOND = FadeRequest.create(ONE_SECOND, 0, 1, 0);
    private static final FadeRequest FADE_OUT_TWO_STEPS = FadeRequest.create(TWO_STEPS, 0, 1, 0);
    private static final FadeRequest FADE_IN_TWO_STEPS = FadeRequest.create(TWO_STEPS, 0, 0, 1);

    @Mock FadeHandler.Listener listener;

    private FadeHandler fadeHandler;
    private TestScheduler scheduler = new TestScheduler();

    @Before
    public void setUp() throws Exception {
        fadeHandler = new FadeHandler(listener, scheduler);
    }

    @Test
    public void fadeEmitsEvents() throws Exception {
        int expectedSteps = (int) (ONE_SECOND / STEP_MS) + 1;

        fadeHandler.fade(FADE_OUT_ONE_SECOND);
        scheduler.advanceTimeBy(TWO_SECONDS, TimeUnit.MILLISECONDS);

        InOrder inOrder = Mockito.inOrder(listener);
        inOrder.verify(listener, times(expectedSteps)).onFade(anyLong());
        inOrder.verify(listener).onFadeFinished();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void fadeOutAcceleratesVolume() throws Exception {
        fadeHandler.fade(FADE_OUT_TWO_STEPS);
        scheduler.advanceTimeBy(TWO_STEPS, TimeUnit.MILLISECONDS);

        InOrder inOrder = Mockito.inOrder(listener);
        inOrder.verify(listener).onFade(1f);
        inOrder.verify(listener).onFade(0.75f);
        inOrder.verify(listener).onFade(0f);
        inOrder.verify(listener).onFadeFinished();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void fadeInDeceleratesVolume() throws Exception {
        fadeHandler.fade(FADE_IN_TWO_STEPS);
        scheduler.advanceTimeBy(TWO_STEPS, TimeUnit.MILLISECONDS);

        InOrder inOrder = Mockito.inOrder(listener);
        inOrder.verify(listener).onFade(0f);
        inOrder.verify(listener).onFade(0.75f);
        inOrder.verify(listener).onFade(1f);
        inOrder.verify(listener).onFadeFinished();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void fadeDelaysWithNegativeOffsets() throws Exception {
        FadeRequest fadeRequest = FadeRequest.create(TWO_STEPS, -ONE_SECOND, 1, 0);

        fadeHandler.fade(fadeRequest);
        scheduler.advanceTimeBy(HALF_SECOND, TimeUnit.MILLISECONDS);

        Mockito.inOrder(listener).verifyNoMoreInteractions();
    }

    @Test
    public void fadeDelaysWithNegativeOffsetsStartsAfterDelay() throws Exception {
        FadeRequest fadeRequest = FadeRequest.create(TWO_STEPS, -ONE_SECOND, 1, 0);

        fadeHandler.fade(fadeRequest);
        scheduler.advanceTimeBy(TWO_SECONDS, TimeUnit.MILLISECONDS);

        InOrder inOrder = Mockito.inOrder(listener);
        inOrder.verify(listener, times(3)).onFade(anyLong());
        inOrder.verify(listener).onFadeFinished();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void fadeWithPositiveOffsetsStartsAfterOffset() throws Exception {
        FadeRequest fadeRequest = FadeRequest.create(TWO_STEPS, STEP_MS, 1, 0);

        fadeHandler.fade(fadeRequest);
        scheduler.advanceTimeBy(HALF_SECOND, TimeUnit.MILLISECONDS);

        InOrder inOrder = Mockito.inOrder(listener);
        inOrder.verify(listener).onFade(0.75f);
        inOrder.verify(listener).onFade(0f);
        inOrder.verify(listener).onFadeFinished();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void fadeWithZeroDurationFiresEventsImmediately() throws Exception {
        FadeRequest fadeRequest = FadeRequest.create(0, 0, 1, 0);

        fadeHandler.fade(fadeRequest);

        InOrder inOrder = Mockito.inOrder(listener);
        inOrder.verify(listener).onFade(0f);
        inOrder.verify(listener).onFadeFinished();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void stopFadeCancelsExistingFade() throws Exception {
        fadeHandler.fade(FADE_OUT_TWO_STEPS);
        scheduler.advanceTimeBy(STEP_MS, TimeUnit.MILLISECONDS);

        fadeHandler.stop();
        scheduler.advanceTimeBy(ONE_SECOND, TimeUnit.MILLISECONDS);

        InOrder inOrder = Mockito.inOrder(listener);
        inOrder.verify(listener).onFade(1f);
        inOrder.verify(listener).onFade(0.75f);
        inOrder.verify(listener).onFadeFinished();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void fadeWhileFadingStopsExistingFade() throws Exception {
        fadeHandler.fade(FADE_OUT_TWO_STEPS);
        scheduler.advanceTimeBy(STEP_MS, TimeUnit.MILLISECONDS);

        fadeHandler.fade(FADE_IN_TWO_STEPS);
        scheduler.advanceTimeBy(ONE_SECOND, TimeUnit.MILLISECONDS);

        InOrder inOrder = Mockito.inOrder(listener);
        inOrder.verify(listener).onFade(1f);
        inOrder.verify(listener).onFade(0.75f);
        inOrder.verify(listener).onFadeFinished();
        inOrder.verify(listener).onFade(0f);
        inOrder.verify(listener).onFade(0.75f);
        inOrder.verify(listener).onFade(1f);
        inOrder.verify(listener).onFadeFinished();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void fadeEmitsImmediatelyWhenOffsetIsPositive() throws Exception {
        FadeRequest fadeRequest = FadeRequest.create(TWO_STEPS, STEP_MS, 1, 0);

        fadeHandler.fade(fadeRequest);
        scheduler.advanceTimeBy(0, TimeUnit.MILLISECONDS);

        InOrder inOrder = Mockito.inOrder(listener);
        inOrder.verify(listener).onFade(0.75f);
        inOrder.verifyNoMoreInteractions();
    }

}
