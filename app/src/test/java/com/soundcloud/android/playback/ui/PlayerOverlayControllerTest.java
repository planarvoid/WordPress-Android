package com.soundcloud.android.playback.ui;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.soundcloud.android.ads.AdOverlayController;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import android.view.View;

@RunWith(MockitoJUnitRunner.class)
public class PlayerOverlayControllerTest {

    public static final Urn TRACK_URN = TestPlayStates.URN;
    private PlayerOverlayController controller;

    @Mock private OverlayAnimator overlayAnimator;
    @Mock private View overlay;
    @Mock private AdOverlayController adOverlayController;

    @Before
    public void setUp() {
        controller = new PlayerOverlayController(overlay, overlayAnimator);
    }

    @Test
    public void shouldHideOverlayImmediatelyOnSetExpandedWhilePlaying() {
        setPlayingState();
        controller.setAlphaFromCollapse(0);
        verify(overlayAnimator).setAlpha(overlay, 0);
    }

    @Test
    public void shouldNotHideOverlayImmediatelyOnSetExpandedWhilePlayingIfBlocked() {
        setPlayingState();
        controller.setBlocked(true);
        controller.setAlphaFromCollapse(0);
        verify(overlayAnimator, never()).setAlpha(overlay, 0);
    }

    @Test
    public void shouldShowOverlayImmediatelyOnSetCollapsedWhilePlaying() {
        setPlayingState();
        controller.setAlphaFromCollapse(1);
        verify(overlayAnimator).setAlpha(overlay, 1);
    }

    @Test
    public void shouldShowOverlayWhileScrubbingExpandedAndPlaying() {
        setPlayingState();
        controller.setAlphaFromCollapse(0);
        Mockito.reset(overlayAnimator);

        controller.scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);

        verify(overlayAnimator).showOverlay(overlay);
    }

    @Test
    public void shouldHideOverlayOnSetExpandedWhilePlayingAfterScrubStateSetToNone() {
        setPlayingState();
        controller.setAlphaFromCollapse(0);
        controller.scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);
        Mockito.reset(overlayAnimator);

        controller.scrubStateChanged(ScrubController.SCRUB_STATE_NONE);

        verify(overlayAnimator).hideOverlay(overlay);
    }

    @Test
    public void shouldNotHideOverlayOnSetExpandedWhilePlayingAfterScrubStateSetToNoneIfBlocked() {
        setPlayingState();
        controller.setBlocked(true);
        controller.setAlphaFromCollapse(0);
        controller.scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);
        Mockito.reset(overlayAnimator);

        controller.scrubStateChanged(ScrubController.SCRUB_STATE_NONE);

        verify(overlayAnimator, never()).hideOverlay(overlay);
    }

    @Test
    public void shouldHideOverlayOnSetExpandedWhilePlayingAfterScrubStateCancelled() {
        setPlayingState();
        controller.setAlphaFromCollapse(0);
        controller.scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);
        Mockito.reset(overlayAnimator);

        controller.scrubStateChanged(ScrubController.SCRUB_STATE_CANCELLED);

        verify(overlayAnimator).hideOverlay(overlay);
    }

    @Test
    public void shouldNotHideOverlayOnSetExpandedWhilePlayingAfterScrubStateCancelledIfBlocked() {
        setPlayingState();
        controller.setBlocked(true);
        controller.setAlphaFromCollapse(0);
        controller.scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);
        Mockito.reset(overlayAnimator);

        controller.scrubStateChanged(ScrubController.SCRUB_STATE_CANCELLED);

        verify(overlayAnimator, never()).hideOverlay(overlay);
    }

    @Test
    public void shouldNotHideTheOverlayOnPlayingStateWhenAdOverlayDisplayed() {
        controller.setAdOverlayShown(true);

        setPlayingState();

        verify(overlayAnimator, never()).hideOverlay(overlay);
    }

    private void setPlayingState() {
        controller.setPlayState(TestPlayStates.playing());
    }

    @Test
    public void shouldNotHideTheOverlayWhileExpandingWhenAdOverlayDisplayed() {
        controller.setAlphaFromCollapse(0);

        verifyZeroInteractions(overlayAnimator);
    }

    @Test
    public void shouldNotHideTheOverlayOnAdOverlayHiddenWhenIdle() {
        setIdleState();

        controller.setAdOverlayShown(false);

        verify(overlayAnimator, never()).hideOverlay(overlay);
    }

    @Test
    public void shouldShowTheOverlayOnAdOverlayShownWhenPlaying() {
        setPlayingState();

        controller.setAdOverlayShown(true);

        verify(overlayAnimator).showOverlay(overlay);
    }

    @Test
    public void shouldShowTheOverlayOnAdOverlayShownWhenIdle() {
        setIdleState();

        controller.setAdOverlayShown(true);

        verify(overlayAnimator, times(2)).showOverlay(overlay);
    }

    @Test
    public void shouldShowOverlayOnShowIdleState() {
        setIdleState();

        verify(overlayAnimator).showOverlay(overlay);
    }

    private void setIdleState() {
        controller.setPlayState(TestPlayStates.idle());
    }

    @Test
    public void shouldHideOverlayOnShowPlayingStateWhileExpandedAndNotScrubbing() {
        controller.setAlphaFromCollapse(0);
        setPlayingState();

        verify(overlayAnimator).hideOverlay(overlay);
    }

    @Test
    public void shouldNotHideOverlayOnShowPlayingStateWhileExpandedAndNotScrubbing() {
        controller.setBlocked(true);
        controller.setAlphaFromCollapse(0);
        setPlayingState();

        verify(overlayAnimator, never()).hideOverlay(overlay);
    }

    @Test
    public void shouldHideOverlayOnShowPlayingStateWhileExpandedAfterCancellingScrubbing() {
        controller.setAlphaFromCollapse(0);
        controller.scrubStateChanged(ScrubController.SCRUB_STATE_CANCELLED);
        Mockito.reset(overlayAnimator);

        setPlayingState();

        verify(overlayAnimator).hideOverlay(overlay);
    }

    @Test
    public void shouldNotHideOverlayOnShowPlayingStateWhileExpandedAfterCancellingScrubbing() {
        controller.setBlocked(true);
        controller.setAlphaFromCollapse(0);
        controller.scrubStateChanged(ScrubController.SCRUB_STATE_CANCELLED);
        Mockito.reset(overlayAnimator);

        setPlayingState();

        verify(overlayAnimator, never()).hideOverlay(overlay);
    }
}
