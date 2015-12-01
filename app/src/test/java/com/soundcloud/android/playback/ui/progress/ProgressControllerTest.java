package com.soundcloud.android.playback.ui.progress;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.view.View;

import java.util.concurrent.TimeUnit;

public class ProgressControllerTest extends AndroidUnitTest {

    public static final float FLOAT_DELTA = .0001f;
    public static final int FULL_DURATION = 10;
    private ProgressController controller;

    private View progressView;

    private PlaybackProgress progress = PlaybackProgress.empty();

    @Mock
    private TranslateXHelper helper;
    @Mock
    private ProgressAnimator progressAnimator;

    @Before
    public void setUp() throws Exception {
        progressView = new View(context());
        controller = new ProgressController(progressView, helper);

        when(helper.createAnimator(eq(progressView), anyFloat())).thenReturn(progressAnimator);
    }

    @Test
    public void startAnimationDoesNotStartAnimatorWithNoBounds() {
        controller.setHelper(new EmptyProgressHelper());
        controller.startProgressAnimation(progress, FULL_DURATION);
        verify(progressAnimator, never()).start();
    }

    @Test
    public void setsBoundsStartsAnimationIfAnimationPreviouslyRequested() {
        controller.startProgressAnimation(progress, FULL_DURATION);
        verify(progressAnimator).start();
    }

    @Test
    public void startAnimationWithBoundsStartsAnimationOnObjectAnimator() {
        controller.startProgressAnimation(progress, FULL_DURATION);
        verify(progressAnimator).start();
    }

    @Test
    public void startAnimationSetsDurationToTimeLeftThenSetsCurrentPlayTimeToTimeSinceCreation() {
        final TestDateProvider dateProvider = new TestDateProvider(0);
        progress = getPlaybackProgress(0, 1, dateProvider);

        dateProvider.setTime(20, TimeUnit.MILLISECONDS);

        controller.setPlaybackProgress(progress, FULL_DURATION);
        controller.startProgressAnimation(progress, FULL_DURATION);

        InOrder inOrder = Mockito.inOrder(progressAnimator);
        inOrder.verify(progressAnimator).setDuration(10L);
        inOrder.verify(progressAnimator).start();
        inOrder.verify(progressAnimator).setCurrentPlayTime(20L);
    }

    private PlaybackProgress getPlaybackProgress(int position, int duration) {
        return getPlaybackProgress(position, duration, new TestDateProvider());
    }

    private PlaybackProgress getPlaybackProgress(int position, int duration, TestDateProvider dateProvider) {
        return new PlaybackProgress(position, duration, dateProvider);
    }

    @Test
    public void cancelProgressAnimationDoesNothingWhenAnimatorIsNull() {
        controller.cancelProgressAnimation();
        verifyZeroInteractions(progressAnimator);
    }

    @Test
    public void cancelProgressAnimationCallsCancelOnAnimatorWhenItExists() {
        when(helper.createAnimator(progressView, (float) 20)).thenReturn(progressAnimator);
        controller.startProgressAnimation(progress, FULL_DURATION);
        controller.cancelProgressAnimation();
        verify(progressAnimator).cancel();
    }

    @Test
    public void setProgressSetsValueOnHelperIfAnimationIsNull() {
        controller.setPlaybackProgress(getPlaybackProgress(2, 8), 10);
        verify(helper).setValueFromProportion(progressView, .2f);
    }

    @Test
    public void setProgressSetsValueOnHelperIfAnimationIsNotRunning() {
        when(progressAnimator.isRunning()).thenReturn(false);
        progress = getPlaybackProgress(2, 8);

        controller.startProgressAnimation(progress, FULL_DURATION);
        controller.setPlaybackProgress(this.progress, 10);

        verify(helper).setValueFromProportion(progressView, .2f);
    }

    @Test
    public void setProgressRestartsAnimationIfStrayedMoreThanTolerance() {
        when(progressAnimator.isRunning()).thenReturn(true);
        controller.startProgressAnimation(progress, FULL_DURATION);
        progress = getPlaybackProgress(3, 8);

        when(helper.getValueFromProportion(AdditionalMatchers.eq(.3f, FLOAT_DELTA))).thenReturn(100f);
        when(progressAnimator.getDifferenceFromCurrentValue(AdditionalMatchers.eq(100f, FLOAT_DELTA))).thenReturn(2f);
        final ProgressAnimator secondAnimator = Mockito.mock(TranslateXAnimator.class);
        when(helper.createAnimator(same(progressView), AdditionalMatchers.eq(.3f, FLOAT_DELTA)))
                .thenReturn(secondAnimator);

        controller.setPlaybackProgress(progress, FULL_DURATION);
        verify(secondAnimator).start();
    }

    @Test
    public void setProgressCancelsOldAnimationIfStrayedMoreThanTolerance() {
        when(progressAnimator.isRunning()).thenReturn(true);
        controller.startProgressAnimation(progress, FULL_DURATION);
        progress = getPlaybackProgress(3, 8);

        when(helper.getValueFromProportion(AdditionalMatchers.eq(.3f, FLOAT_DELTA))).thenReturn(100f);
        when(progressAnimator.getDifferenceFromCurrentValue(AdditionalMatchers.eq(100f, FLOAT_DELTA))).thenReturn(2f);
        when(helper.createAnimator(same(progressView), AdditionalMatchers.eq(.3f, FLOAT_DELTA)))
                .thenReturn(Mockito.mock(TranslateXAnimator.class));

        controller.setPlaybackProgress(progress, FULL_DURATION);
        verify(progressAnimator).cancel();
    }

    @Test
    public void setProgressDoesNothingIfStrayedLessThanTolerance() {
        when(progressAnimator.isRunning()).thenReturn(true);
        controller.startProgressAnimation(progress, FULL_DURATION);
        progress = getPlaybackProgress(3, 8);

        when(progressAnimator.getDifferenceFromCurrentValue(30)).thenReturn(.9f);

        final ProgressAnimator secondAnimator = Mockito.mock(TranslateXAnimator.class);
        when(helper.createAnimator(any(View.class), anyFloat())).thenReturn(secondAnimator);
        controller.setPlaybackProgress(progress, FULL_DURATION);

        verify(progressAnimator, never()).cancel();
        verifyZeroInteractions(secondAnimator);
    }

    @Test
    public void setHelperAfterResettingDoesNotStartAnimation() {
        controller.startProgressAnimation(progress, FULL_DURATION);
        Mockito.reset(progressAnimator);

        controller.reset();
        controller.setHelper(helper);

        verify(progressAnimator, never()).start();

    }
}
