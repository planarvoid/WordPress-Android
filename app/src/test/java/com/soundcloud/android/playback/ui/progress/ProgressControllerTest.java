package com.soundcloud.android.playback.ui.progress;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.testsupport.PlatformUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.view.View;

public class ProgressControllerTest extends PlatformUnitTest {

    public static final float FLOAT_DELTA = .0001f;
    private ProgressController controller;

    private View progressView;

    @Mock
    private PlaybackProgress progress;
    @Mock
    private TranslateXHelper helper;
    @Mock
    private ProgressAnimator progressAnimator;


    @Before
    public void setUp() throws Exception {
        progressView = new View(context());
        controller = new ProgressController(progressView, helper);

        when(progress.getProgressProportion()).thenReturn(.2f);
        when(helper.createAnimator(eq(progressView), anyFloat())).thenReturn(progressAnimator);
    }

    @Test
    public void startAnimationDoesNotStartAnimatorWithNoBounds() {
        controller.setHelper(new EmptyProgressHelper());
        controller.startProgressAnimation(progress);
        verify(progressAnimator, never()).start();
    }

    @Test
    public void setsBoundsStartsAnimationIfAnimationPreviouslyRequested() {
        controller.startProgressAnimation(progress);
        verify(progressAnimator).start();
    }

    @Test
    public void startAnimationWithBoundsStartsAnimationOnObjectAnimator() {
        controller.startProgressAnimation(progress);
        verify(progressAnimator).start();
    }

    @Test
    public void startAnimationSetsDurationToTimeLeftThenSetsCurrentPlayTimeToTimeSinceCreation() {
        when(progress.getTimeLeft()).thenReturn(10L);
        when(progress.getTimeSinceCreation()).thenReturn(20L);

        controller.startProgressAnimation(progress);

        InOrder inOrder = inOrder(progressAnimator);
        inOrder.verify(progressAnimator).setDuration(10L);
        inOrder.verify(progressAnimator).start();
        inOrder.verify(progressAnimator).setCurrentPlayTime(20L);
    }

    @Test
    public void cancelProgressAnimationDoesNothingWhenAnimatorIsNull() {
        controller.cancelProgressAnimation();
        verifyZeroInteractions(progressAnimator);
    }

    @Test
    public void cancelProgressAnimationCallsCancelOnAnimatorWhenItExists() {
        when(progress.getProgressProportion()).thenReturn(.2f);
        when(helper.createAnimator(progressView, (float) 20)).thenReturn(progressAnimator);
        controller.startProgressAnimation(progress);
        controller.cancelProgressAnimation();
        verify(progressAnimator).cancel();
    }

    @Test
    public void setProgressSetsValueOnHelperIfAnimationIsNull() {
        when(progress.getProgressProportion()).thenReturn(.2f);
        controller.setPlaybackProgress(progress);
        verify(helper).setValueFromProportion(progressView, .2f);
    }

    @Test
    public void setProgressSetsValueOnHelperIfAnimationIsNotRunning() {
        when(progress.getProgressProportion()).thenReturn(.2f);
        when(progressAnimator.isRunning()).thenReturn(false);
        controller.startProgressAnimation(progress);

        controller.setPlaybackProgress(progress);
        verify(helper).setValueFromProportion(progressView, .2f);
    }

    @Test
    public void setProgressRestartsAnimationIfStrayedMoreThanTolerance() {
        when(progress.getProgressProportion()).thenReturn(.2f);
        when(progressAnimator.isRunning()).thenReturn(true);
        controller.startProgressAnimation(progress);

        when(progress.getProgressProportion()).thenReturn(.3f);
        when(helper.getValueFromProportion(AdditionalMatchers.eq(.3f, FLOAT_DELTA))).thenReturn(100f);
        when(progressAnimator.getDifferenceFromCurrentValue(AdditionalMatchers.eq(100f, FLOAT_DELTA))).thenReturn(2f);
        final ProgressAnimator secondAnimator = Mockito.mock(TranslateXAnimator.class);
        when(helper.createAnimator(same(progressView), AdditionalMatchers.eq(.3f, FLOAT_DELTA)))
                .thenReturn(secondAnimator);

        controller.setPlaybackProgress(progress);
        verify(secondAnimator).start();
    }

    @Test
    public void setProgressCancelsOldAnimationIfStrayedMoreThanTolerance() {
        when(progress.getProgressProportion()).thenReturn(.2f);
        when(progressAnimator.isRunning()).thenReturn(true);
        controller.startProgressAnimation(progress);

        when(helper.getValueFromProportion(AdditionalMatchers.eq(.3f, FLOAT_DELTA))).thenReturn(100f);
        when(progressAnimator.getDifferenceFromCurrentValue(AdditionalMatchers.eq(100f, FLOAT_DELTA))).thenReturn(2f);
        when(progress.getProgressProportion()).thenReturn(.3f);
        when(helper.createAnimator(same(progressView), AdditionalMatchers.eq(.3f, FLOAT_DELTA)))
                .thenReturn(Mockito.mock(TranslateXAnimator.class));

        controller.setPlaybackProgress(progress);
        verify(progressAnimator).cancel();
    }

    @Test
    public void setProgressDoesNothingIfStrayedLessThanTolerance() {
        when(progress.getProgressProportion()).thenReturn(.2f);
        when(progressAnimator.isRunning()).thenReturn(true);
        controller.startProgressAnimation(progress);

        when(progressAnimator.getDifferenceFromCurrentValue(30)).thenReturn(.9f);
        when(progress.getProgressProportion()).thenReturn(.3f);

        final ProgressAnimator secondAnimator = Mockito.mock(TranslateXAnimator.class);
        when(helper.createAnimator(any(View.class), anyFloat())).thenReturn(secondAnimator);
        controller.setPlaybackProgress(progress);

        verify(progressAnimator, never()).cancel();
        verifyZeroInteractions(secondAnimator);
    }

    @Test
    public void setHelperAfterResettingDoesNotStartAnimation() {
        controller.startProgressAnimation(progress);
        Mockito.reset(progressAnimator);

        controller.reset();
        controller.setHelper(helper);

        verify(progressAnimator, never()).start();

    }
}