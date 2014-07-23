package com.soundcloud.android.playback.ui;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class PlayerOverlayControllerTest {

    private PlayerOverlayController controller;

    @Mock private OverlayAnimator overlayAnimator;
    @Mock private PlaySessionStateProvider playSessionController;
    @Mock private View overlay;

    @Before
    public void setUp() throws Exception {
        controller = new PlayerOverlayController(overlayAnimator, playSessionController);
    }

    @Test
    public void showSessionShouldHideOverlay() {
        controller.showSessionActiveState(overlay);
        verify(overlayAnimator).hideOverlay(overlay);
    }

    @Test
    public void showSessionWhenDarkeningShouldNotHideOverlay() {
        controller.darken(overlay);
        controller.showSessionActiveState(overlay);

        verify(overlayAnimator, never()).hideOverlay(overlay);
    }

    @Test
    public void showSessionActivateOnPlayStateShouldHideOverlay() {
        controller.showSessionActiveState(overlay);

        verify(overlayAnimator).hideOverlay(overlay);
    }

    @Test
    public void shouldHideOverlayWhenSessionIsPlaying() {
        when(playSessionController.isPlaying()).thenReturn(true);
        controller.hideOverlay(overlay);

        verify(overlayAnimator).hideOverlay(overlay);
    }

    @Test
    public void shouldNotHideOverlayWhenSessionIsIdle() {
        when(playSessionController.isPlaying()).thenReturn(false);
        controller.hideOverlay(overlay);

        verify(overlayAnimator, never()).hideOverlay(overlay);
    }

    @Test
    public void showIdleStateShouldShowOverlay() {
        controller.showIdleState(overlay);

        verify(overlayAnimator).showOverlay(overlay);
    }

    @Test
    public void darkenShouldShowOverlay() {
        controller.darken(overlay);

        verify(overlayAnimator).showOverlay(overlay);
    }
}