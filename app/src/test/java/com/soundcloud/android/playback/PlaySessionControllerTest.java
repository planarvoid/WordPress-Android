package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.base.Predicate;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.service.managers.IRemoteAudioManager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.track.LegacyTrackOperations;
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
    private static final TrackUrn TRACK_URN = Urn.forTrack(TRACK_ID);
    private static final Track TRACK = new Track(TRACK_ID);

    @Mock
    private PlaybackOperations playbackOperations;
    @Mock
    private PlayQueueManager playQueueManager;
    @Mock
    private Resources resources;
    @Mock
    private LegacyTrackOperations trackOperations;
    @Mock
    private Lazy<IRemoteAudioManager> audioManagerProvider;
    @Mock
    private IRemoteAudioManager audioManager;
    @Mock
    private ImageOperations imageOperations;
    @Mock
    private Bitmap bitmap;

    private PlaySessionController controller;
    private TestEventBus eventBus = new TestEventBus();


    @Before
    public void setUp() throws Exception {
        when(audioManagerProvider.get()).thenReturn(audioManager);
        controller = new PlaySessionController(resources, eventBus, playbackOperations, playQueueManager, trackOperations, audioManagerProvider, imageOperations);
        controller.subscribe();

        when(playQueueManager.getCurrentTrackId()).thenReturn(TRACK_ID);
        when(trackOperations.loadTrack(TRACK_ID, AndroidSchedulers.mainThread())).thenReturn(Observable.just(TRACK));
    }

    @Test
    public void isPlayingTrackReturnsFalseIfNoPlayStateEventIsReceived() {
        expect(controller.isPlayingTrack(TRACK)).toBeFalse();
    }

    @Test
    public void isPlayingTrackReturnsTrueIfLastTransitionHappenedOnTheTargetTrack() {
        sendIdleStateEvent();

        expect(controller.isPlayingTrack(TRACK)).toBeTrue();
    }

    @Test
    public void isGetCurrentProgressReturns0IfCurrentTrackDidNotStartPlaying() {
        sendIdleStateEvent();

        expect(controller.getCurrentProgress(TRACK_URN).getPosition()).toEqual(0L);
    }

    @Test
    public void playQueueChangedHandlerCallsPlayCurrentOnPlaybackOperationsIfThePlayerIsInPlaySession() {
        final Playa.StateTransition lastTransition = Mockito.mock(Playa.StateTransition.class);
        when(lastTransition.playSessionIsActive()).thenReturn(true);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, lastTransition);
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(TRACK.getUrn()));

        verify(playbackOperations).playCurrent();
    }

    @Test
    public void playQueueChangedEventForUpdateDoesNotCallPlayCurrentOnPlaybackOperationsIfThePlayerIsInPlaySession() {
        final Playa.StateTransition lastTransition = Mockito.mock(Playa.StateTransition.class);
        when(lastTransition.playSessionIsActive()).thenReturn(true);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, lastTransition);
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate(TRACK.getUrn()));

        verify(playbackOperations, never()).playCurrent();
    }

    @Test
    public void playQueueChangedHandlerDoesNotCallPlayCurrentIfPlaySessionIsNotActive() {
        final Playa.StateTransition lastTransition = Mockito.mock(Playa.StateTransition.class);
        when(lastTransition.playSessionIsActive()).thenReturn(false);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, lastTransition);
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(TRACK.getUrn()));

        verify(playbackOperations, never()).playCurrent();
    }

    @Test
    public void playQueueChangedHandlerDoesNotSetTrackOnAudioManagerIfTrackChangeNotSupported() {
        when(audioManager.isTrackChangeSupported()).thenReturn(false);
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(TRACK.getUrn()));
        verify(audioManager, never()).onTrackChanged(any(Track.class), any(Bitmap.class));
    }

    @Test
    public void playQueueChangedHandlerSetsLockScreenStateWithBitmapForCurrentTrack() {
        when(audioManager.isTrackChangeSupported()).thenReturn(true);
        when(imageOperations.image(TRACK_URN, ApiImageSize.T500, true)).thenReturn(Observable.just(bitmap));

        InOrder inOrder = Mockito.inOrder(audioManager);
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(TRACK.getUrn()));
        inOrder.verify(audioManager).onTrackChanged(TRACK, null);
        inOrder.verify(audioManager).onTrackChanged(TRACK, bitmap);
    }

    @Test
    public void playQueueChangedHandlerSetsLockScreenStateWithNullBitmapForCurrentTrackOnImageLoadError() {
        when(audioManager.isTrackChangeSupported()).thenReturn(true);
        when(imageOperations.image(TRACK_URN, ApiImageSize.T500, true)).thenReturn(Observable.<Bitmap>error(new Exception("Could not load image")));

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(TRACK.getUrn()));
        verify(audioManager).onTrackChanged(TRACK, null);
    }

    @Test
    public void shouldNotRespondToQueueChangesWhenPlayerIsNotPlaying() {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(TRACK.getUrn()));

        verify(playbackOperations, never()).playCurrent();
    }

    @Test
    public void returnsLastProgressEventForCurrentUrnFromEventQueue() throws Exception {
        TrackUrn trackUrn = Urn.forTrack(123L);
        final Playa.StateTransition event = new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, trackUrn);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, event);

        final PlaybackProgressEvent playbackProgressEvent = new PlaybackProgressEvent(new PlaybackProgress(1L, 2L), trackUrn);
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, playbackProgressEvent);
        expect(controller.getCurrentProgress()).toBe(playbackProgressEvent.getPlaybackProgress());
    }

    @Test
    public void returnsLastProgressEventByUrnFromEventQueue() throws Exception {
        
        final PlaybackProgressEvent playbackProgressEvent = new PlaybackProgressEvent(new PlaybackProgress(1L, 2L), TRACK_URN);
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, playbackProgressEvent);
        expect(controller.getCurrentProgress(TRACK_URN)).toBe(playbackProgressEvent.getPlaybackProgress());
    }

    @Test
    public void returnsEmptyProgressByUrnIfNoProgressReceived() throws Exception {
        expect(controller.getCurrentProgress(TRACK_URN)).toEqual(PlaybackProgress.empty());
    }

    @Test
    public void isProgressInitialSecondsReturnsTrueIfProgressLessThatThreeSeconds() {
        sendIdleStateEvent();
        final PlaybackProgress playbackProgress = new PlaybackProgress(2999L, 42L);
        final PlaybackProgressEvent playbackProgressEvent = new PlaybackProgressEvent(playbackProgress, TRACK_URN);
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, playbackProgressEvent);
        expect(controller.isProgressWithinTrackChangeThreshold()).toBeTrue();
    }

    @Test
    public void isProgressInitialSecondsReturnsFalseIfProgressEqualToThreeSeconds() {
        sendIdleStateEvent();
        final PlaybackProgress playbackProgress = new PlaybackProgress(3000L, 42L);
        final PlaybackProgressEvent playbackProgressEvent = new PlaybackProgressEvent(playbackProgress, TRACK_URN);
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, playbackProgressEvent);
        expect(controller.isProgressWithinTrackChangeThreshold()).toBeFalse();
    }

    @Test
    public void isProgressInitialSecondsReturnsFalseIfProgressMoreThanThreeSeconds() {
        sendIdleStateEvent();
        final PlaybackProgress playbackProgress = new PlaybackProgress(3001L, 42L);
        final PlaybackProgressEvent playbackProgressEvent = new PlaybackProgressEvent(playbackProgress, TRACK_URN);
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, playbackProgressEvent);
        expect(controller.isProgressWithinTrackChangeThreshold()).toBeFalse();
    }

    private void sendIdleStateEvent() {
        final Playa.StateTransition lastTransition = new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE, TRACK_URN);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, lastTransition);
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceTracksIfTrackNotEnded() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, TRACK_URN));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE, TRACK_URN));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_FAILED, TRACK_URN));
        verify(playQueueManager, never()).autoNextTrack();
    }

    @Test
    public void onStateTransitionPlaysCurrentTrackAfterAdvancingPlayQueueAfterCompletedTrack() throws Exception {
        when(playQueueManager.autoNextTrack()).thenReturn(true);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.TRACK_COMPLETE, TRACK_URN));
        verify(playbackOperations).playCurrent();
    }

    @Test
    public void onStateTransitionDoesNotOpenCurrentTrackAfterFailingToAdvancePlayQueue() throws Exception {
        when(playQueueManager.autoNextTrack()).thenReturn(false);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.TRACK_COMPLETE, TRACK_URN));
        verifyZeroInteractions(playbackOperations);
    }

    @Test
    public void onStateTransitionPublishesPlayQueueCompleteEventAfterFailingToAdvancePlayQueue() throws Exception {
        when(playQueueManager.autoNextTrack()).thenReturn(false);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.TRACK_COMPLETE, TRACK_URN));
        expect(eventBus.eventsOn(EventQueue.PLAYBACK_STATE_CHANGED, new Predicate<Playa.StateTransition>() {
            @Override
            public boolean apply(Playa.StateTransition event) {
                return event.getNewState() == Playa.PlayaState.IDLE && event.getReason() == Playa.Reason.PLAY_QUEUE_COMPLETE;
            }
        })).toNumber(1);
    }

    @Test
    public void onStateTransitionForTrackEndSavesQueueWithPositionWithZero() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.TRACK_COMPLETE, TRACK_URN, 123, 456));
        verify(playQueueManager).saveCurrentPosition(0);
    }

    @Test
    public void onStateTransitionForReasonNoneSavesQueueWithPositionFromTransition() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE, TRACK_URN, 123, 456));
        verify(playQueueManager).saveCurrentPosition(123);
    }

    @Test
    public void onStateTransitionForWithConsecutivePlaylistEventsSavesProgressOnTrackChange() {
        final Playa.StateTransition state1 = new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, Urn.forTrack(1L));
        final Playa.StateTransition state2 = new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, Urn.forTrack(2L), 123, 456);

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, state1);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, state2);
        verify(playQueueManager).saveCurrentPosition(123);
    }

    @Test
    public void onStateTransitionForQueueCompleteDoesNotSavePosition() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.PLAY_QUEUE_COMPLETE, TRACK_URN));
        verify(playQueueManager, never()).saveCurrentPosition(anyInt());
    }

    @Test
    public void onStateTransitionForBufferingDoesNotSaveQueuePosition() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE, TRACK_URN, 123, 456));
        verify(playQueueManager, never()).saveCurrentPosition(anyInt());
    }

    @Test
    public void onStateTransitionForPlayingDoesNotSaveQueuePosition() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, TRACK_URN, 123, 456));
        verify(playQueueManager, never()).saveCurrentPosition(anyInt());
    }

}