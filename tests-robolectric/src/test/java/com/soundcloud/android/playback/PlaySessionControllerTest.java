package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.audioAdProperties;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedTrackForPlayer;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.base.Predicate;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.service.managers.IRemoteAudioManager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;

import android.content.res.Resources;
import android.graphics.Bitmap;

@RunWith(SoundCloudTestRunner.class)
public class PlaySessionControllerTest {

    private Urn trackUrn;
    private PropertySet track;
    private PropertySet trackWithAdMeta;
    private Bitmap bitmap;

    @Mock private PlaybackOperations playbackOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private Resources resources;
    @Mock private TrackRepository trackRepository;
    @Mock private IRemoteAudioManager audioManager;
    @Mock private ImageOperations imageOperations;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private AdsOperations adsOperations;
    @Mock private CastConnectionHelper castConnectionHelper;

    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
        PlaySessionController controller = new PlaySessionController(resources, eventBus, playbackOperations, playQueueManager, trackRepository,
                InjectionSupport.lazyOf(audioManager), imageOperations, playSessionStateProvider, castConnectionHelper);
        controller.subscribe();

        track = expectedTrackForPlayer();
        trackWithAdMeta = audioAdProperties(Urn.forTrack(123L)).merge(track);
        trackUrn = track.get(TrackProperty.URN);

        when(trackRepository.track(trackUrn)).thenReturn(Observable.just(track));
    }

    @Test
    public void playQueueTrackChangedHandlerCallsPlayCurrentOnPlaybackOperationsIfThePlayerIsInPlaySession() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(trackUrn));

        verify(playbackOperations).playCurrent();
    }

    @Test
    public void playQueueTrackChangedHandlerDoesNotCallPlayCurrentIfPlaySessionIsNotActive() {
        final Playa.StateTransition lastTransition = Mockito.mock(Playa.StateTransition.class);
        when(lastTransition.playSessionIsActive()).thenReturn(false);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, lastTransition);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(trackUrn));

        verify(playbackOperations, never()).playCurrent();
    }

    @Test
    public void playQueueTrackChangeWhenCastingPlaysTrackWhenCurrentTrackIsDifferentAndPlaying() {
        when(castConnectionHelper.isCasting()).thenReturn(true);
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        PropertySet previousCurrentTrack = setupTrackLoad(Urn.forTrack(5L));
        PropertySet newCurrentTrack = setupTrackLoad(Urn.forTrack(6L));
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(previousCurrentTrack.get(TrackProperty.URN)));
        Mockito.reset(playbackOperations);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(newCurrentTrack.get(TrackProperty.URN)));

        verify(playbackOperations).playCurrent();
    }

    @Test
    public void playQueueTrackChangeWhenCastingDoesNotPlayTrackWhenCurrentTrackStaysTheSame() {
        when(castConnectionHelper.isCasting()).thenReturn(true);
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(trackUrn));
        Mockito.reset(playbackOperations);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(trackUrn));

        verify(playbackOperations, never()).playCurrent();
    }

    @Test
    public void playQueueTrackChangedHandlerDoesNotSetTrackOnAudioManagerIfTrackChangeNotSupported() {
        when(audioManager.isTrackChangeSupported()).thenReturn(false);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(trackUrn));
        verify(audioManager, never()).onTrackChanged(any(PropertySet.class), any(Bitmap.class));
    }

    @Test
    public void playQueueChangedHandlerSetsLockScreenStateWithBitmapForCurrentTrack() {
        when(audioManager.isTrackChangeSupported()).thenReturn(true);
        when(imageOperations.artwork(trackUrn, ApiImageSize.T500)).thenReturn(Observable.just(bitmap));

        InOrder inOrder = Mockito.inOrder(audioManager);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(trackUrn));
        inOrder.verify(audioManager).onTrackChanged(track, null);
        inOrder.verify(audioManager).onTrackChanged(track, bitmap);
    }

    @Test
    public void playQueueChangedHandlerSetsLockScreenStateWithBitmapForCurrentAudioAdTrack() {
        when(audioManager.isTrackChangeSupported()).thenReturn(true);
        when(imageOperations.artwork(trackUrn, ApiImageSize.T500)).thenReturn(Observable.just(bitmap));

        InOrder inOrder = Mockito.inOrder(audioManager);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(trackUrn, audioAdProperties(Urn.forTrack(123L))));
        inOrder.verify(audioManager).onTrackChanged(eq(trackWithAdMeta), eq(((Bitmap) null)));
        inOrder.verify(audioManager).onTrackChanged(trackWithAdMeta, bitmap);
    }

    @Test
    public void playQueueTrackChangedHandlerSetsLockScreenStateWithNullBitmapForCurrentTrackOnImageLoadError() {
        when(audioManager.isTrackChangeSupported()).thenReturn(true);
        when(imageOperations.artwork(trackUrn, ApiImageSize.T500)).thenReturn(Observable.<Bitmap>error(new Exception("Could not load image")));

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(trackUrn));
        verify(audioManager).onTrackChanged(track, null);
    }

    @Test
    public void shouldNotRespondToPlayQueueTrackChangesWhenPlayerIsNotPlaying() {
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(trackUrn));

        verify(playbackOperations, never()).playCurrent();
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceTracksIfTrackNotEnded() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, trackUrn));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE, trackUrn));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_FAILED, trackUrn));
        verify(playQueueManager, never()).autoNextTrack();
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceTrackIfTrackEndedWhileCasting() {
        when(castConnectionHelper.isCasting()).thenReturn(true);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.TRACK_COMPLETE, trackUrn));
        verify(playQueueManager, never()).autoNextTrack();
    }

    @Test
    public void onStateTransitionDoesNotOpenCurrentTrackAfterFailingToAdvancePlayQueue() throws Exception {
        when(playQueueManager.autoNextTrack()).thenReturn(false);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.TRACK_COMPLETE, trackUrn));
        verifyZeroInteractions(playbackOperations);
    }

    @Test
    public void onStateTransitionPublishesPlayQueueCompleteEventAfterFailingToAdvancePlayQueue() throws Exception {
        when(playQueueManager.autoNextTrack()).thenReturn(false);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.TRACK_COMPLETE, trackUrn));
        expect(eventBus.eventsOn(EventQueue.PLAYBACK_STATE_CHANGED, new Predicate<Playa.StateTransition>() {
            @Override
            public boolean apply(Playa.StateTransition event) {
                return event.getNewState() == Playa.PlayaState.IDLE && event.getReason() == Playa.Reason.PLAY_QUEUE_COMPLETE;
            }
        })).toNumber(1);
    }

    @Test
    public void onStateTransitionForQueueCompleteDoesNotSavePosition() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.PLAY_QUEUE_COMPLETE, trackUrn));
        verify(playQueueManager, never()).saveCurrentProgress(anyInt());
    }

    @Test
    public void onStateTransitionForBufferingDoesNotSaveQueuePosition() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE, trackUrn, 123, 456));
        verify(playQueueManager, never()).saveCurrentProgress(anyInt());
    }

    @Test
    public void onStateTransitionForPlayingDoesNotSaveQueuePosition() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, trackUrn, 123, 456));
        verify(playQueueManager, never()).saveCurrentProgress(anyInt());
    }

    private PropertySet setupTrackLoad(Urn urn) {
        PropertySet track = PropertySet.from(TrackProperty.URN.bind(urn));
        when(trackRepository.track(urn)).thenReturn(Observable.just(track));
        return track;
    }
}