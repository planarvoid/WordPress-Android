package com.soundcloud.android.playback;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

public class VolumeControllerTest extends AndroidUnitTest {

    private static final long ONE_SECOND = 1000;
    private static final FadeRequest FADE_OUT_ONE_SECOND = FadeRequest.create(ONE_SECOND, 0, 1, 0);
    private static final FadeRequest FADE_IN_ONE_SECOND = FadeRequest.create(ONE_SECOND, 0, 0, 1);
    private static final FadeRequest DUCK_ONE_SECOND = FadeRequest.create(ONE_SECOND, 0, 1, .1f);

    @Mock StreamPlayer streamPlayer;
    @Mock FadeHandlerFactory fadeHandlerFactory;
    @Mock FadeHelper fadeHelper;
    @Mock VolumeController.Listener listener;

    private VolumeController volumeController;

    @Before
    public void setUp() {
        when(fadeHandlerFactory.create(any(VolumeController.class))).thenReturn(fadeHelper);
        volumeController = new VolumeController(streamPlayer, listener, fadeHandlerFactory);
    }

    @Test
    public void muteFadesOutWhenNotMuted() {
        when(streamPlayer.getVolume()).thenReturn(1.0f);

        volumeController.mute(ONE_SECOND);

        verify(fadeHelper).fade(FADE_OUT_ONE_SECOND);
    }

    @Test
    public void muteFadesOutOnlyOnce() {
        when(streamPlayer.getVolume()).thenReturn(1.0f);

        volumeController.mute(ONE_SECOND);
        volumeController.mute(ONE_SECOND);

        verify(fadeHelper, times(1)).fade(FADE_OUT_ONE_SECOND);
    }

    @Test
    public void unMuteFadesInWhenMuted() {
        when(streamPlayer.getVolume()).thenReturn(1f);
        volumeController.mute(ONE_SECOND);
        when(streamPlayer.getVolume()).thenReturn(0f);

        volumeController.unMute(ONE_SECOND);

        verify(fadeHelper).fade(FADE_IN_ONE_SECOND);
    }

    @Test
    public void unMuteDoesNotFadeInWhenNotMuted() {
        when(streamPlayer.getVolume()).thenReturn(0f);

        volumeController.unMute(ONE_SECOND);

        verify(fadeHelper, never()).fade(FADE_IN_ONE_SECOND);
    }

    @Test
    public void unMuteFadesInOnlyOnce() {
        when(streamPlayer.getVolume()).thenReturn(1f);
        volumeController.mute(ONE_SECOND);
        when(streamPlayer.getVolume()).thenReturn(0f);

        volumeController.unMute(ONE_SECOND);
        volumeController.unMute(ONE_SECOND);

        InOrder inOrder = Mockito.inOrder(fadeHelper);
        inOrder.verify(fadeHelper).fade(FADE_OUT_ONE_SECOND);
        inOrder.verify(fadeHelper).fade(FADE_IN_ONE_SECOND);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void duckFadesOutToDuckVolume() {
        when(streamPlayer.getVolume()).thenReturn(1f);

        volumeController.duck(ONE_SECOND);

        verify(fadeHelper).fade(DUCK_ONE_SECOND);
    }

    @Test
    public void duckDoesNotFadeOutWhenMuted() {
        when(streamPlayer.getVolume()).thenReturn(1f);
        volumeController.mute(ONE_SECOND);

        volumeController.duck(ONE_SECOND);

        verify(fadeHelper, never()).fade(DUCK_ONE_SECOND);
    }

    @Test
    public void duckDoesNotFadeOutWhenAlreadyDucked() {
        when(streamPlayer.getVolume()).thenReturn(1f);

        volumeController.duck(ONE_SECOND);
        volumeController.duck(ONE_SECOND);

        verify(fadeHelper, times(1)).fade(DUCK_ONE_SECOND);
    }

    @Test
    public void resetVolumeSetsVolumeToMaximum() {
        volumeController.resetVolume();

        verify(streamPlayer).setVolume(1f);
    }

    @Test
    public void resetVolumeStopsAnyOngoingFade() {
        volumeController.resetVolume();

        verify(fadeHelper).stop();
    }

    @Test
    public void resetVolumeDoesNothingWhenMuted() {
        volumeController.mute(ONE_SECOND);

        volumeController.resetVolume();

        verify(streamPlayer, never()).setVolume(1f);
    }

    @Test
    public void resetVolumeDoesNothingWhenDucked() {
        volumeController.duck(ONE_SECOND);

        volumeController.resetVolume();

        verify(streamPlayer, never()).setVolume(1f);
    }

    @Test
    public void fadeOutSetsDurationAndOffset() {
        FadeRequest fadeRequest = FadeRequest.create(ONE_SECOND, -ONE_SECOND, 1, 0);
        when(streamPlayer.getVolume()).thenReturn(1.0f);

        volumeController.fadeOut(ONE_SECOND, -ONE_SECOND);

        verify(fadeHelper).fade(fadeRequest);
    }

    @Test
    public void onFadePausedCallsListener() {
        volumeController.onFadeFinished();

        verify(listener).onFadeFinished();
    }

    @Test
    public void onFadeSetsVolumeWhenFadingOut() {
        volumeController.fadeOut(ONE_SECOND, 0);

        volumeController.onFade(0.5f);

        verify(streamPlayer).setVolume(0.5f);
    }

    @Test
    public void onFadeSetsVolumeWhenFadingIn() {
        volumeController.fadeIn(ONE_SECOND, 0);

        volumeController.onFade(0.2f);

        verify(streamPlayer).setVolume(0.2f);
    }

    @Test
    public void onFadeDoesNotSetVolumeWhenNotFading() {
        volumeController.onFade(0.2f);

        verify(streamPlayer, never()).setVolume(0.2f);
    }

}
