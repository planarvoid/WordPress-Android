package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.service.managers.IRemoteAudioManager;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.track.TrackOperations;
import dagger.Lazy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

import android.content.res.Resources;
import android.graphics.Bitmap;

@RunWith(SoundCloudTestRunner.class)
public class PlaySessionControllerTest {

    private static final long TRACK_ID = 123L;
    private static final TrackUrn TRACK_URN = TrackUrn.forTrack(TRACK_ID);
    @Mock
    private EventBus eventBus;
    @Mock
    private PlaybackOperations playbackOperations;
    @Mock
    private PlayQueueManager playQueueManager;
    @Mock
    private Resources resources;
    @Mock
    private TrackOperations trackOperations;
    @Mock
    private Lazy<IRemoteAudioManager> audioManagerProvider;
    @Mock
    private IRemoteAudioManager audioManager;
    @Mock
    private ImageOperations imageOperations;
    @Mock
    private Track track;
    @Mock
    private Bitmap bitmap;

    private  PlaySessionController controller;
    private EventMonitor monitor;


    @Before
    public void setUp() throws Exception {
        when(audioManagerProvider.get()).thenReturn(audioManager);
        controller = new PlaySessionController(resources, eventBus, playbackOperations, playQueueManager, trackOperations, audioManagerProvider, imageOperations);
        monitor = EventMonitor.on(eventBus);
        controller.subscribe();

        when(track.getUrn()).thenReturn(TRACK_URN);
        when(playQueueManager.getCurrentTrackId()).thenReturn(TRACK_ID);
        when(trackOperations.loadTrack(TRACK_ID, AndroidSchedulers.mainThread())).thenReturn(Observable.just(track));
    }

    @Test
    public void shouldSubscribeToThePlayStateEventQueue() {
        monitor.verifySubscribedTo(EventQueue.PLAYBACK_STATE_CHANGED);
    }

    @Test
    public void shouldSubscribeToThePlaybackProgressEventQueue() {
        monitor.verifySubscribedTo(EventQueue.PLAYBACK_PROGRESS);
    }

    @Test
    public void shouldSubscribeToThePlayQueueEventQueue() {
        monitor.verifySubscribedTo(EventQueue.PLAY_QUEUE);
    }

    @Test
    public void playQueueChangedHandlerCallsPlayCurrentOnPlaybackOperationsIfThePlayerIsInPlaySession() {
        final Playa.StateTransition lastTransition = Mockito.mock(Playa.StateTransition.class);
        when(lastTransition.playSessionIsActive()).thenReturn(true);
        monitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, lastTransition);
        monitor.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(track.getUrn()));

        verify(playbackOperations).playCurrent();
    }

    @Test
    public void playQueueChangedHandlerDoesNotCallPlayCurrentIfPlaySessionIsNotActive() {
        final Playa.StateTransition lastTransition = Mockito.mock(Playa.StateTransition.class);
        when(lastTransition.playSessionIsActive()).thenReturn(false);
        monitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, lastTransition);
        monitor.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(track.getUrn()));

        verify(playbackOperations, never()).playCurrent();
    }

    @Test
    public void playQueueChangedHandlerDoesNotSetTrackOnAudioManagerIfTrackChangeNotSupported() {
        when(audioManager.isTrackChangeSupported()).thenReturn(false);
        monitor.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(track.getUrn()));
        verify(audioManager, never()).onTrackChanged(any(Track.class), any(Bitmap.class));
    }

    @Test
    public void playQueueChangedHandlerSetsLockScreenStateWithBitmapForCurrentTrack() {
        when(audioManager.isTrackChangeSupported()).thenReturn(true);
        when(imageOperations.image(TRACK_URN, ApiImageSize.T500, true)).thenReturn(Observable.just(bitmap));

        InOrder inOrder = Mockito.inOrder(audioManager);
        monitor.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(track.getUrn()));
        inOrder.verify(audioManager).onTrackChanged(track, null);
        inOrder.verify(audioManager).onTrackChanged(track, bitmap);
    }

    @Test
    public void playQueueChangedHandlerSetsLockScreenStateWithNullBitmapForCurrentTrackOnImageLoadError() {
        when(audioManager.isTrackChangeSupported()).thenReturn(true);
        when(imageOperations.image(TRACK_URN, ApiImageSize.T500, true)).thenReturn(Observable.<Bitmap>error(new Exception("Could not load image")));

        monitor.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(track.getUrn()));
        verify(audioManager).onTrackChanged(track, null);
    }

    @Test
    public void shouldNotRespondToQueueChangesWhenPlayerIsNotPlaying() {
        monitor.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(track.getUrn()));

        verify(playbackOperations, never()).playCurrent();
    }

    @Test
    public void returnsLastProgressEventFromEventQueue() throws Exception {
        final PlaybackProgressEvent playbackProgressEvent = new PlaybackProgressEvent(1L, 2L);
        monitor.publish(EventQueue.PLAYBACK_PROGRESS, playbackProgressEvent);
        expect(controller.getCurrentProgress()).toBe(playbackProgressEvent);
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceTracksIfTrackNotEnded() throws Exception {
        monitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE));
        monitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE));
        monitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_FAILED));
        verifyZeroInteractions(playQueueManager);
    }

    @Test
    public void onStateTransitionPlaysCurrentTrackAfterAdvancingPlayQueueAfterCompletedTrack() throws Exception {
        when(playQueueManager.autoNextTrack()).thenReturn(true);
        monitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.TRACK_COMPLETE));
        verify(playbackOperations).playCurrent();
    }

    @Test
    public void onStateTransitionDoesNotOpenCurrentTrackAfterFailingToAdvancePlayQueue() throws Exception {
        when(playQueueManager.autoNextTrack()).thenReturn(false);
        monitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.TRACK_COMPLETE));
        verifyZeroInteractions(playbackOperations);
    }

    @Test
    public void onStateTransitionPublishesPlayQueueCompleteEventAfterFailingToAdvancePlayQueue() throws Exception {
        when(playQueueManager.autoNextTrack()).thenReturn(false);
        monitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.TRACK_COMPLETE));
        Playa.StateTransition event = monitor.verifyEventOn(EventQueue.PLAYBACK_STATE_CHANGED);
        expect(event.getNewState()).toBe(Playa.PlayaState.IDLE);
        expect(event.getReason()).toBe(Playa.Reason.PLAY_QUEUE_COMPLETE);
    }
}