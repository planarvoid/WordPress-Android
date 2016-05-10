package com.soundcloud.android.playback.mediasession;

import static android.content.Intent.ACTION_MEDIA_BUTTON;
import static android.content.Intent.ACTION_MEDIA_EJECT;
import static android.content.Intent.EXTRA_KEY_EVENT;
import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.ACTION_UP;
import static android.view.KeyEvent.KEYCODE_HEADSETHOOK;
import static android.view.KeyEvent.KEYCODE_MEDIA_PLAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.external.PlaybackAction;
import com.soundcloud.android.playback.external.PlaybackActionController;
import com.soundcloud.android.playback.external.PlaybackActionReceiver;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.schedulers.TestScheduler;

import android.content.Intent;
import android.view.KeyEvent;

import java.util.concurrent.TimeUnit;

public class MediaSessionListenerTest extends AndroidUnitTest {

    private static final Intent HOOK_DOWN_INTENT = buildMediaButtonIntent(ACTION_DOWN, KEYCODE_HEADSETHOOK);
    private static final Intent HOOK_UP_INTENT = buildMediaButtonIntent(ACTION_UP, KEYCODE_HEADSETHOOK);

    @Mock MediaSessionController controller;
    @Mock PlaybackActionController actionController;

    private TestScheduler scheduler = new TestScheduler();
    private MediaSessionListener listener;

    @Before
    public void setUp() {
        listener = new MediaSessionListener(controller, actionController, context(), scheduler);
    }

    @Test
    public void onPauseSendsActionPause() {
        listener.onPause();

        verifyAction(PlaybackAction.PAUSE);
    }

    @Test
    public void onPlaySendsActionPlay() {
        listener.onPlay();

        verifyAction(PlaybackAction.PLAY);
    }

    @Test
    public void onPlayDoesNotSendsActionPlayWhenPlayingVideoAd() {
        when(controller.isPlayingVideoAd()).thenReturn(true);

        listener.onPlay();

        verifyNoAction(PlaybackAction.PLAY);
    }

    @Test
    public void onStopSendsActionClose() {
        listener.onStop();

        verifyAction(PlaybackAction.CLOSE);
    }

    @Test
    public void onSkipToNextSendsActionNext() {
        listener.onSkipToNext();

        verifyAction(PlaybackAction.NEXT);
    }

    @Test
    public void onSkipToNextNotifiesController() {
        listener.onSkipToNext();

        verify(controller).onSkip();
    }

    @Test
    public void onSkipToPreviousSendsActionPrevious() {
        listener.onSkipToPrevious();

        verifyAction(PlaybackAction.PREVIOUS);
    }

    @Test
    public void onSkipToPreviousNotifiesController() {
        listener.onSkipToPrevious();

        verify(controller).onSkip();
    }

    @Test
    public void onMediaButtonEventHandlesHeadsetHookKeyDown() {
        boolean handled = listener.onMediaButtonEvent(HOOK_DOWN_INTENT);

        assertThat(handled).isTrue();
    }

    @Test
    public void onMediaButtonEventHandlesHeadsetHookKeyUp() {
        boolean handled = listener.onMediaButtonEvent(HOOK_UP_INTENT);

        assertThat(handled).isTrue();
    }

    @Test
    public void onMediaButtonEventDoesNotHandlesOtherMediaKeys() {
        Intent intent = buildMediaButtonIntent(ACTION_DOWN, KEYCODE_MEDIA_PLAY);

        boolean handled = listener.onMediaButtonEvent(intent);

        assertThat(handled).isFalse();
    }

    @Test
    public void onMediaButtonEventDoesNotHandlesOtherActions() {
        Intent intent = new Intent(ACTION_MEDIA_EJECT);

        boolean handled = listener.onMediaButtonEvent(intent);

        assertThat(handled).isFalse();
    }

    @Test
    public void onMediaButtonEventWaitsForTimeout() {
        clickHeadsetHook();

        verifyNoAction(PlaybackAction.TOGGLE_PLAYBACK);
    }

    @Test
    public void onMediaButtonEventTogglesPlaybackWithOneClickAfterTimeout() {
        clickHeadsetHookAndAdvanceAfterTimeout();

        verifyAction(PlaybackAction.TOGGLE_PLAYBACK);
    }

    @Test
    public void onMediaButtonEventSkipsToNextWithTwoClicksWithinTimeout() {
        clickHeadsetHookAndAdvanceBeforeTimeout();
        clickHeadsetHookAndAdvanceAfterTimeout();

        verifyAction(PlaybackAction.NEXT);
    }

    @Test
    public void onMediaButtonEventSkipsToPreviousWithThreeClicksWithoutWaitingForTimeout() {
        clickHeadsetHookAndAdvanceBeforeTimeout();
        clickHeadsetHookAndAdvanceBeforeTimeout();

        clickHeadsetHook();

        verifyAction(PlaybackAction.PREVIOUS);
    }

    @Test
    public void onMediaButtonEventDoesNothingAfterClickingMoreThanThreeTimesWithinTimeout() {
        clickHeadsetHookAndAdvanceBeforeTimeout();
        clickHeadsetHookAndAdvanceBeforeTimeout();
        clickHeadsetHookAndAdvanceBeforeTimeout();
        clickHeadsetHookAndAdvanceBeforeTimeout();
        clickHeadsetHookAndAdvanceAfterTimeout();

        verifyAction(PlaybackAction.PREVIOUS, 1);
        verifyNoAction(PlaybackAction.TOGGLE_PLAYBACK);
        verifyNoAction(PlaybackAction.NEXT);
    }

    @Test
    public void onMediaButtonEventResetsAfterTimeout() {
        clickHeadsetHookAndAdvanceAfterTimeout();
        clickHeadsetHookAndAdvanceAfterTimeout();

        verifyAction(PlaybackAction.TOGGLE_PLAYBACK, 2);
    }

    @Test
    public void onMediaButtonEventIgnoresKeyUps() {
        clickHeadsetHookAndAdvanceAfterTimeout();
        listener.onMediaButtonEvent(HOOK_UP_INTENT);
        scheduler.advanceTimeBy(MediaSessionListener.HEADSET_DELAY_MS, TimeUnit.MILLISECONDS);

        verifyAction(PlaybackAction.TOGGLE_PLAYBACK, 1);
    }

    private static Intent buildMediaButtonIntent(int keyAction, int keyCode) {
        KeyEvent keyEvent = new KeyEvent(keyAction, keyCode);
        return new Intent(ACTION_MEDIA_BUTTON).putExtra(EXTRA_KEY_EVENT, keyEvent);
    }

    private void verifyNoAction(String action) {
        verify(actionController, never())
                .handleAction(action, PlaybackActionReceiver.SOURCE_REMOTE);
    }

    private void verifyAction(String action) {
        verifyAction(action, 1);
    }

    private void verifyAction(String action, int count) {
        verify(actionController, times(count))
                .handleAction(action, PlaybackActionReceiver.SOURCE_REMOTE);
    }

    private void clickHeadsetHook() {
        listener.onMediaButtonEvent(HOOK_DOWN_INTENT);
    }

    private void clickHeadsetHookAndAdvanceAfterTimeout() {
        clickHeadsetHook();
        scheduler.advanceTimeBy(MediaSessionListener.HEADSET_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void clickHeadsetHookAndAdvanceBeforeTimeout() {
        clickHeadsetHook();
        scheduler.advanceTimeBy(MediaSessionListener.HEADSET_DELAY_MS - 1, TimeUnit.MILLISECONDS);
    }

}
