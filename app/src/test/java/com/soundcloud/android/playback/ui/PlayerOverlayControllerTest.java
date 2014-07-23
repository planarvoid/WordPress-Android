package com.soundcloud.android.playback.ui;

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
    @Mock private PlaySessionStateProvider playStateProvider;
    @Mock private View overlay;

    @Before
    public void setUp() throws Exception {
        controller = new PlayerOverlayController(overlay, overlayAnimator, playStateProvider);
    }

    @Test
    public void shouldHideOverlayOnSetExpandedWhilePlaying() {
        when(playStateProvider.isPlaying()).thenReturn(true);
        controller.setExpandedAndUpdate();
        verify(overlayAnimator).hideOverlay(overlay);
    }

    @Test
    public void shouldShowOverlayOnSetExpandedWhileNotPlaying() {
        when(playStateProvider.isPlaying()).thenReturn(false);
        controller.setExpandedAndUpdate();
        verify(overlayAnimator).showOverlay(overlay);
    }

    @Test
    public void shouldShowOverlayOnSetCollapsedWhilePlaying() {
        when(playStateProvider.isPlaying()).thenReturn(true);
        controller.setCollapsedAndUpdate();
        verify(overlayAnimator).showOverlay(overlay);
    }

    @Test
    public void shouldShowOverlayOnSetCollapsedWhileNotPlaying() {
        when(playStateProvider.isPlaying()).thenReturn(false);
        controller.setCollapsedAndUpdate();
        verify(overlayAnimator).showOverlay(overlay);
    }

}