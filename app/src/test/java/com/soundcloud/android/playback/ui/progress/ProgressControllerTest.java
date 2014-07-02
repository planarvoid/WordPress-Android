package com.soundcloud.android.playback.ui.progress;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class ProgressControllerTest {

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
        progressView = new View(Robolectric.application);
        controller = new ProgressController(progressView, helper);

        when(progress.getProgressProportion()).thenReturn(.2f);
        when(helper.createAnimator(progressView, .2f)).thenReturn(progressAnimator);
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
    public void startAnimationSetsDurationToProgressTimeRemaining() {
        when(progress.getTimeLeft()).thenReturn(10L);
        controller.startProgressAnimation(progress);
        verify(progressAnimator).setDuration(10L);
    }

    @Test
    public void startAnimationSetsCurrentPlayTimeToTimeSinceCreation() {
        when(progress.getTimeSinceCreation()).thenReturn(20L);
        controller.startProgressAnimation(progress);
        verify(progressAnimator).setCurrentPlayTime(20L);
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
        when(progressAnimator.getDifferenceFromCurrentValue(AdditionalMatchers.eq(.3f, FLOAT_DELTA))).thenReturn(2f);
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

        when(progressAnimator.getDifferenceFromCurrentValue(AdditionalMatchers.eq(.3f, FLOAT_DELTA))).thenReturn(2f);
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
}