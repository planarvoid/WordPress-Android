package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.primitives.Longs;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.appwidget.AppWidgetManager;
import android.content.Intent;

import java.util.List;

@RunWith(DefaultTestRunner.class)
public class PlaybackReceiverTest {

    private PlaybackReceiver playbackReceiver;

    @Mock
    private PlaybackService playbackService;
    @Mock
    private PlayQueueView playQueue;
    @Mock
    private AccountOperations accountOperations;
    @Mock
    private PlayQueueManager playQueueManager;
    @Mock
    private EventBus eventBus;
    @Mock
    private PlaySessionSource playSessionSource;

    @Before
    public void setup() {
        SoundCloudApplication.sModelManager.clear();
        playbackReceiver = new PlaybackReceiver(playbackService, accountOperations, playQueueManager, eventBus);
        when(accountOperations.soundCloudAccountExists()).thenReturn(true);
        when(playbackService.getPlayQueueOriginScreen()).thenReturn("screen_tag");
    }

    @Test
    public void nextActionShouldCallNextAndOpenCurrentIfNextSuccessful() {
        when(playbackService.next()).thenReturn(true);
        playbackReceiver.onReceive(Robolectric.application, new Intent(PlaybackService.Actions.NEXT_ACTION));
        verify(playbackService).openCurrent();
    }

    @Test
    public void nextActionShouldCallNextAndNotOpenCurrentIfNextNotSuccessful() {
        when(playbackService.next()).thenReturn(false);
        playbackReceiver.onReceive(Robolectric.application, new Intent(PlaybackService.Actions.NEXT_ACTION));
        verify(playbackService).next();
        verify(playbackService, never()).openCurrent();
    }

    @Test
    public void prevActionShouldCallPrevAndOpenCurrentIfPrevSuccessful() {
        when(playbackService.prev()).thenReturn(true);
        playbackReceiver.onReceive(Robolectric.application, new Intent(PlaybackService.Actions.PREVIOUS_ACTION));
        verify(playbackService).openCurrent();
    }

    @Test
    public void prevActionShouldCallPrevAndNotOpenCurrentIfPrevNotSuccessful() {
        when(playbackService.prev()).thenReturn(false);
        playbackReceiver.onReceive(Robolectric.application, new Intent(PlaybackService.Actions.PREVIOUS_ACTION));
        verify(playbackService).prev();
        verify(playbackService, never()).openCurrent();
    }

    @Test
    public void togglePlaybackActionShouldCallNextOnService() {
        playbackReceiver.onReceive(Robolectric.application, new Intent(PlaybackService.Actions.TOGGLEPLAYBACK_ACTION));
        verify(playbackService).togglePlayback();
    }

    @Test
    public void pauseActionShouldCallNextOnService() {
        playbackReceiver.onReceive(Robolectric.application, new Intent(PlaybackService.Actions.PAUSE_ACTION));
        verify(playbackService).pause();
    }

    @Test
    public void updateAppWidgetProviderActionShouldCallUpdateOnAppWidgetProviderWithPlaystateChangedAction(){
        Intent intent = new Intent(PlaybackService.Broadcasts.UPDATE_WIDGET_ACTION);
        final int[] ids = {1, 2, 3};
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);

        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playbackService).notifyChange(PlaybackService.Broadcasts.PLAYSTATE_CHANGED);
    }

    @Test
    public void shouldCreateAndSetPlayQueueOnPlayQueueManager() {
        final long[] idListArray = new long[]{1L, 2L, 3L};
        final List<Long> idList = Longs.asList(idListArray);

        Intent intent = new Intent(PlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(PlaybackService.PlayExtras.TRACK_ID_LIST, idListArray);
        intent.putExtra(PlaybackService.PlayExtras.PLAY_SESSION_SOURCE, playSessionSource);
        intent.putExtra(PlaybackService.PlayExtras.START_POSITION, 2);

        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playQueueManager).setNewPlayQueue(eq(PlayQueue.fromIdList(idList, 2, playSessionSource)), eq(playSessionSource));
    }

    @Test
    public void sendingNewPlayQueueShouldOpenCurrentTrackInPlaybackService() {
        Intent intent = new Intent(PlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(PlaybackService.PlayExtras.TRACK_ID_LIST, new long[]{1L, 2L, 3L});
        intent.putExtra(PlaybackService.PlayExtras.PLAY_SESSION_SOURCE, playSessionSource);
        intent.putExtra(PlaybackService.PlayExtras.START_POSITION, 2);

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(playbackService).openCurrent();
    }

    @Test
    public void sendingNewPlayQueueShouldFetchRelatedTracksIfLoadRecommendedExtraIsTrue() {
        Intent intent = new Intent(PlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(PlaybackService.PlayExtras.TRACK_ID_LIST, new long[]{1L});
        intent.putExtra(PlaybackService.PlayExtras.PLAY_SESSION_SOURCE, playSessionSource);
        intent.putExtra(PlaybackService.PlayExtras.START_POSITION, 0);
        intent.putExtra(PlaybackService.PlayExtras.LOAD_RECOMMENDED, true);

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(playQueueManager).fetchRelatedTracks(1L);
    }

    @Test
    public void sendingNewPlayQueueShouldNotFetchRelatedTracksIfLoadRecommendedExtraIsFalse() {
        Intent intent = new Intent(PlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(PlaybackService.PlayExtras.TRACK_ID_LIST, new long[]{1L});
        intent.putExtra(PlaybackService.PlayExtras.PLAY_SESSION_SOURCE, playSessionSource);
        intent.putExtra(PlaybackService.PlayExtras.START_POSITION, 0);
        intent.putExtra(PlaybackService.PlayExtras.LOAD_RECOMMENDED, false);

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(playQueueManager, never()).fetchRelatedTracks(anyLong());
    }

    @Test
    public void shouldCallResetAllOnServiceAndClearPlayqueueOnResetAllAction(){
        Intent intent = new Intent(PlaybackService.Actions.RESET_ALL);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playbackService).resetAll();
        verify(playQueueManager).clearAll();
    }

    @Test
    public void shouldCallSaveProgressAndStopOnStopActionIfPlaying(){
        when(playbackService.isSupposedToBePlaying()).thenReturn(true);
        Intent intent = new Intent(PlaybackService.Actions.STOP_ACTION);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playbackService).saveProgressAndStop();
    }

    @Test
    public void shouldCallStopOnStopActionIfNotPlaying(){
        when(playbackService.isSupposedToBePlaying()).thenReturn(false);
        Intent intent = new Intent(PlaybackService.Actions.STOP_ACTION);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playbackService).stop();
    }

    @Test
    public void shouldNotCallSaveProgressAndStopOnStopActionIfNotPlaying(){
        when(playbackService.isSupposedToBePlaying()).thenReturn(false);
        Intent intent = new Intent(PlaybackService.Actions.STOP_ACTION);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playbackService, never()).saveProgressAndStop();
    }

    @Test
    public void shouldCallResetAllWithNoAccount(){
        Intent intent = new Intent(PlaybackService.Actions.RESET_ALL);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playbackService).resetAll();
        verify(playQueueManager).clearAll();
        verifyZeroInteractions(accountOperations);
    }

    @Test
    public void shouldOpenCurrentIfPlayQueueChangedFromEmptyPlaylist(){
        when(playbackService.isWaitingForPlaylist()).thenReturn(true);
        Intent intent = new Intent(PlaybackService.Broadcasts.PLAYQUEUE_CHANGED);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playbackService, never()).saveProgressAndStop();
    }

    @Test
    public void shouldNotInteractWithThePlayBackServiceIfNoAccountExists(){
        when(accountOperations.soundCloudAccountExists()).thenReturn(false);
        Intent intent = new Intent(PlaybackService.Broadcasts.PLAYQUEUE_CHANGED);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verifyZeroInteractions(playbackService);
    }

    @Test
    public void shouldNotInteractWithThePlayqueueManagerIfNoAccountExists(){
        when(accountOperations.soundCloudAccountExists()).thenReturn(false);
        Intent intent = new Intent(PlaybackService.Broadcasts.PLAYQUEUE_CHANGED);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verifyZeroInteractions(playQueue);
    }

    @Test
    public void shouldTrackTogglePlayEventWithSource() {
        when(playbackService.isSupposedToBePlaying()).thenReturn(false);
        setupTrackingTest(PlaybackService.Actions.TOGGLEPLAYBACK_ACTION, "source");

        ArgumentCaptor<PlayControlEvent> captor = ArgumentCaptor.forClass(PlayControlEvent.class);
        verify(eventBus).publish(eq(EventQueue.PLAY_CONTROL), captor.capture());
        PlayControlEvent event = captor.getValue();
        expect(event.getAttributes().get("action")).toEqual("play");
        expect(event.getAttributes().get("location")).toEqual("source");
    }

    @Test
     public void shouldTrackTogglePauseEventWithSource() {
        when(playbackService.isSupposedToBePlaying()).thenReturn(true);
        setupTrackingTest(PlaybackService.Actions.TOGGLEPLAYBACK_ACTION, "source");

        ArgumentCaptor<PlayControlEvent> captor = ArgumentCaptor.forClass(PlayControlEvent.class);
        verify(eventBus).publish(eq(EventQueue.PLAY_CONTROL), captor.capture());
        PlayControlEvent event = captor.getValue();
        expect(event.getAttributes().get("action")).toEqual("pause");
        expect(event.getAttributes().get("location")).toEqual("source");
    }

    @Test
    public void shouldTrackPlayEventWithSource() {
        setupTrackingTest(PlaybackService.Actions.PLAY_ACTION, "source");

        ArgumentCaptor<PlayControlEvent> captor = ArgumentCaptor.forClass(PlayControlEvent.class);
        verify(eventBus).publish(eq(EventQueue.PLAY_CONTROL), captor.capture());
        PlayControlEvent event = captor.getValue();
        expect(event.getAttributes().get("action")).toEqual("play");
        expect(event.getAttributes().get("location")).toEqual("source");
    }

    @Test
    public void shouldTrackPauseEventWithSource() {
        setupTrackingTest(PlaybackService.Actions.PAUSE_ACTION, "source");

        ArgumentCaptor<PlayControlEvent> captor = ArgumentCaptor.forClass(PlayControlEvent.class);
        verify(eventBus).publish(eq(EventQueue.PLAY_CONTROL), captor.capture());
        PlayControlEvent event = captor.getValue();
        expect(event.getAttributes().get("action")).toEqual("pause");
        expect(event.getAttributes().get("location")).toEqual("source");
    }

    @Test
    public void shouldTrackSkipEventWithSource() {
        setupTrackingTest(PlaybackService.Actions.NEXT_ACTION, "source");

        ArgumentCaptor<PlayControlEvent> captor = ArgumentCaptor.forClass(PlayControlEvent.class);
        verify(eventBus).publish(eq(EventQueue.PLAY_CONTROL), captor.capture());
        PlayControlEvent event = captor.getValue();
        expect(event.getAttributes().get("action")).toEqual("skip");
        expect(event.getAttributes().get("location")).toEqual("source");
    }

    @Test
    public void shouldTrackPreviousEventWithSource() {
        setupTrackingTest(PlaybackService.Actions.PREVIOUS_ACTION, "source");

        ArgumentCaptor<PlayControlEvent> captor = ArgumentCaptor.forClass(PlayControlEvent.class);
        verify(eventBus).publish(eq(EventQueue.PLAY_CONTROL), captor.capture());
        PlayControlEvent event = captor.getValue();
        expect(event.getAttributes().get("action")).toEqual("prev");
        expect(event.getAttributes().get("location")).toEqual("source");
    }

    private void setupTrackingTest(String action, String source) {
        when(accountOperations.soundCloudAccountExists()).thenReturn(false);
        Intent intent = new Intent(action).putExtra(PlayControlEvent.EXTRA_EVENT_SOURCE, source);
        playbackReceiver.onReceive(Robolectric.application, intent);
    }

}
