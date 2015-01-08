package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.content.Intent;
import android.media.AudioManager;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackReceiverTest {

    private PlaybackReceiver playbackReceiver;

    private TestEventBus eventBus = new TestEventBus();

    @Mock private PlaybackService playbackService;
    @Mock private PlayQueueView playQueue;
    @Mock private AccountOperations accountOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionSource playSessionSource;

    @Before
    public void setup() {
        playbackReceiver = new PlaybackReceiver.Factory().create(playbackService, accountOperations, playQueueManager, eventBus);
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        when(playQueueManager.getScreenTag()).thenReturn("screen_tag");
    }

    @Test
    public void togglePlaybackActionShouldCallNextOnService() {
        playbackReceiver.onReceive(Robolectric.application, new Intent(PlaybackService.Actions.TOGGLEPLAYBACK_ACTION));
        verify(playbackService).togglePlayback();
    }

    @Test
    public void playCurrentActionShouldCallOpenCurrentOnService() {
        playbackReceiver.onReceive(Robolectric.application, new Intent(PlaybackService.Actions.PLAY_CURRENT));
        verify(playbackService).openCurrent();
    }

    @Test
    public void pauseActionShouldCallNextOnService() {
        playbackReceiver.onReceive(Robolectric.application, new Intent(PlaybackService.Actions.PAUSE_ACTION));
        verify(playbackService).pause();
    }

    @Test
    public void shouldCallPauseOnServiceOnAudioBecomingNoisyAction() {
        Intent intent = new Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(playbackService).pause();
    }

    @Test
    public void seekActionShouldCallSeekOnService() {
        Intent intent = new Intent(PlaybackService.Actions.SEEK);
        intent.putExtra(PlaybackService.ActionsExtras.SEEK_POSITION, 63L);

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(playbackService).seek(63L, true);
    }

    @Test
    public void shouldCallResetAllOnServiceAndClearPlayqueueOnResetAllAction() {
        Intent intent = new Intent(PlaybackService.Actions.RESET_ALL);
        playbackReceiver.onReceive(Robolectric.application, intent);

        InOrder inOrder = Mockito.inOrder(playQueueManager, playbackService);
        inOrder.verify(playQueueManager).clearAll();
        inOrder.verify(playbackService).resetAll();
    }

    @Test
    public void shouldCallStopOnStopAction() {
        Intent intent = new Intent(PlaybackService.Actions.STOP_ACTION);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playbackService).stop();
    }

    @Test
    public void shouldCallResetAllWithNoAccount() {
        Intent intent = new Intent(PlaybackService.Actions.RESET_ALL);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playbackService).resetAll();
        verify(playQueueManager).clearAll();
        verifyZeroInteractions(accountOperations);
    }

    @Test
    public void shouldOpenCurrentIfPlayQueueChangedFromEmptyPlaylist() {
        when(playbackService.isWaitingForPlaylist()).thenReturn(true);
        Intent intent = new Intent(PlayQueueManager.PLAYQUEUE_CHANGED_ACTION);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playbackService, never()).stop();
    }

    @Test
    public void shouldNotInteractWithThePlayBackServiceIfNoAccountExists() {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        Intent intent = new Intent(PlayQueueManager.PLAYQUEUE_CHANGED_ACTION);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verifyZeroInteractions(playbackService);
    }

    @Test
    public void shouldNotInteractWithThePlayqueueManagerIfNoAccountExists() {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        Intent intent = new Intent(PlayQueueManager.PLAYQUEUE_CHANGED_ACTION);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verifyZeroInteractions(playQueue);
    }

    @Test
    public void shouldTrackPlayEventWithSource() {
        setupTrackingTest(PlaybackService.Actions.PLAY_ACTION, "source");

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.get("action")).toEqual("play");
        expect(event.get("location")).toEqual("source");
    }

    @Test
    public void shouldTrackPauseEventWithSource() {
        setupTrackingTest(PlaybackService.Actions.PAUSE_ACTION, "source");

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.get("action")).toEqual("pause");
        expect(event.get("location")).toEqual("source");
    }

    private void setupTrackingTest(String action, String source) {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        Intent intent = new Intent(action).putExtra(PlayControlEvent.EXTRA_EVENT_SOURCE, source);
        playbackReceiver.onReceive(Robolectric.application, intent);
    }

}
