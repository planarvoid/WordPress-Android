package com.soundcloud.android.playback.ui;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class PlayerOverlayControllerTest {

    private PlayerOverlayController controller;

    @Mock private OverlayAnimator overlayAnimator;
    @Mock private PlaySessionStateProvider playStateProvider;
    @Mock private View overlay;

    @Before
    public void setUp() throws Exception {
        controller = new PlayerOverlayController(overlay, overlayAnimator, playStateProvider);
    }

    @Test
    public void shouldHideOverlayImmediatelyOnSetExpandedWhilePlaying() {
        when(playStateProvider.isPlaying()).thenReturn(true);
        controller.setAlphaFromCollapse(0);
        verify(overlayAnimator).setAlpha(overlay, 0);
    }

    @Test
    public void shouldShowOverlayImmediatelyOnSetCollapsedWhilePlaying() {
        when(playStateProvider.isPlaying()).thenReturn(true);
        controller.setAlphaFromCollapse(1);
        verify(overlayAnimator).setAlpha(overlay, 1);
    }

    @Test
    public void shouldShowOverlayWhileScrubbingExpandedAndPlaying() {
        when(playStateProvider.isPlaying()).thenReturn(true);
        controller.setAlphaFromCollapse(0);
        Mockito.reset(overlayAnimator);

        controller.scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);

        verify(overlayAnimator).showOverlay(overlay);
    }

    @Test
    public void shouldHideOverlayOnSetExpandedWhilePlayingAfterScrubStateSetToNone() {
        when(playStateProvider.isPlaying()).thenReturn(true);
        controller.setAlphaFromCollapse(0);
        controller.scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);
        Mockito.reset(overlayAnimator);

        controller.scrubStateChanged(ScrubController.SCRUB_STATE_NONE);

        verify(overlayAnimator).hideOverlay(overlay);
    }

    @Test
    public void shouldHideOverlayOnSetExpandedWhilePlayingAfterScrubStateCancelled() {
        when(playStateProvider.isPlaying()).thenReturn(true);
        controller.setAlphaFromCollapse(0);
        controller.scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);
        Mockito.reset(overlayAnimator);

        controller.scrubStateChanged(ScrubController.SCRUB_STATE_CANCELLED);

        verify(overlayAnimator).hideOverlay(overlay);
    }

    @Test
    public void shouldShowOverlayOnShowIdleState() {
        controller.showIdleState();
        verify(overlayAnimator).showOverlay(overlay);
    }

    @Test
    public void shouldHideOverlayOnShowPlayingStateWhileExpandedAndNotScrubbing() {
        controller.setAlphaFromCollapse(0);
        controller.showPlayingState();

        verify(overlayAnimator).hideOverlay(overlay);
    }

    @Test
    public void shouldHideOverlayOnShowPlayingStateWhileExpandedAfterCancellingScrubbing() {
        controller.setAlphaFromCollapse(0);
        controller.scrubStateChanged(ScrubController.SCRUB_STATE_CANCELLED);
        Mockito.reset(overlayAnimator);

        controller.showPlayingState();

        verify(overlayAnimator).hideOverlay(overlay);
    }

}