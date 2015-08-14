package com.soundcloud.android.playback;

import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.audioAdProperties;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedTrackForPlayer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.settings.SettingKey;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.TestUrns;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Predicate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;

import java.io.IOException;
import java.util.List;

public class PlaySessionControllerTest extends AndroidUnitTest {
    private final PlayQueue recommendedPlayQueue = PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L,2L), PlaySessionSource.EMPTY);

    private Urn trackUrn;
    private PropertySet track;
    private PropertySet trackWithAdMeta;
    private Bitmap bitmap;

    @Mock private PlaybackOperations playbackOperations;
    @Mock private PlayQueueOperations playQueueOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private Resources resources;
    @Mock private TrackRepository trackRepository;
    @Mock private IRemoteAudioManager audioManager;
    @Mock private ImageOperations imageOperations;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private AdsOperations adsOperations;
    @Mock private CastConnectionHelper castConnectionHelper;
    @Mock private SharedPreferences sharedPreferences;

    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
        PlaySessionController controller = new PlaySessionController(resources, eventBus, playbackOperations, playQueueManager, trackRepository,
                InjectionSupport.lazyOf(audioManager), playQueueOperations, imageOperations, playSessionStateProvider, castConnectionHelper, sharedPreferences);
        controller.subscribe();

        track = expectedTrackForPlayer();
        trackWithAdMeta = audioAdProperties(Urn.forTrack(123L)).merge(track);
        trackUrn = track.get(TrackProperty.URN);

        when(trackRepository.track(trackUrn)).thenReturn(Observable.just(track));

        when(playQueueManager.getLastTrackUrn()).thenReturn(trackUrn);
        when(playQueueOperations.relatedTracksPlayQueue(trackUrn, true)).thenReturn(Observable.just(recommendedPlayQueue));
        when(sharedPreferences.getBoolean(SettingKey.AUTOPLAY_RELATED_ENABLED, true)).thenReturn(true);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(PlaySessionSource.EMPTY);
    }

    @Test
    public void playQueueTrackChangedHandlerCallsPlayCurrentOnPlaybackOperationsIfThePlayerIsInPlaySession() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(trackUrn, Urn.NOT_SET, 0));

        verify(playbackOperations).playCurrent();
    }

    @Test
    public void playQueueTrackChangedHandlerDoesNotCallPlayCurrentIfPlaySessionIsNotActive() {
        final Playa.StateTransition lastTransition = Mockito.mock(Playa.StateTransition.class);
        when(lastTransition.playSessionIsActive()).thenReturn(false);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, lastTransition);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(trackUrn, Urn.NOT_SET, 0));

        verify(playbackOperations, never()).playCurrent();
    }

    @Test
    public void playQueueTrackChangeWhenCastingPlaysTrackWhenCurrentTrackIsDifferentAndPlaying() {
        when(castConnectionHelper.isCasting()).thenReturn(true);
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        PropertySet previousCurrentTrack = setupTrackLoad(Urn.forTrack(5L));
        PropertySet newCurrentTrack = setupTrackLoad(Urn.forTrack(6L));
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(previousCurrentTrack.get(TrackProperty.URN), Urn.NOT_SET, 0));
        Mockito.reset(playbackOperations);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(newCurrentTrack.get(TrackProperty.URN), Urn.NOT_SET, 0));

        verify(playbackOperations).playCurrent();
    }

    @Test
    public void playQueueTrackChangeWhenCastingDoesNotPlayTrackWhenCurrentTrackStaysTheSame() {
        when(castConnectionHelper.isCasting()).thenReturn(true);
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(trackUrn, Urn.NOT_SET, 0));
        Mockito.reset(playbackOperations);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(trackUrn, Urn.NOT_SET, 0));

        verify(playbackOperations, never()).playCurrent();
    }

    @Test
    public void playQueueTrackChangedHandlerDoesNotSetTrackOnAudioManagerIfTrackChangeNotSupported() {
        when(audioManager.isTrackChangeSupported()).thenReturn(false);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(trackUrn, Urn.NOT_SET, 0));
        verify(audioManager, never()).onTrackChanged(any(PropertySet.class), any(Bitmap.class));
    }

    @Test
    public void playQueueChangedHandlerSetsLockScreenStateWithBitmapForCurrentTrack() {
        when(audioManager.isTrackChangeSupported()).thenReturn(true);
        when(imageOperations.artwork(trackUrn, ApiImageSize.T500)).thenReturn(Observable.just(bitmap));

        InOrder inOrder = Mockito.inOrder(audioManager);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(trackUrn, Urn.NOT_SET, 0));
        inOrder.verify(audioManager).onTrackChanged(track, null);
        inOrder.verify(audioManager).onTrackChanged(eq(track), any(Bitmap.class));
    }

    @Test
    public void playQueueChangedHandlerSetsLockScreenStateWithBitmapForCurrentAudioAdTrack() {
        when(audioManager.isTrackChangeSupported()).thenReturn(true);
        when(imageOperations.artwork(trackUrn, ApiImageSize.T500)).thenReturn(Observable.just(bitmap));

        InOrder inOrder = Mockito.inOrder(audioManager);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(trackUrn, Urn.NOT_SET, audioAdProperties(Urn.forTrack(123L)), 0));
        inOrder.verify(audioManager).onTrackChanged(eq(trackWithAdMeta), eq(((Bitmap) null)));
        inOrder.verify(audioManager).onTrackChanged(eq(trackWithAdMeta), any(Bitmap.class));
    }

    @Test
    public void playQueueTrackChangedHandlerSetsLockScreenStateWithNullBitmapForCurrentTrackOnImageLoadError() {
        when(audioManager.isTrackChangeSupported()).thenReturn(true);
        when(imageOperations.artwork(trackUrn, ApiImageSize.T500)).thenReturn(Observable.<Bitmap>error(new Exception("Could not load image")));

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(trackUrn, Urn.NOT_SET, 0));
        verify(audioManager).onTrackChanged(track, null);
    }

    @Test
    public void shouldNotRespondToPlayQueueTrackChangesWhenPlayerIsNotPlaying() {
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(trackUrn, Urn.NOT_SET, 0));

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
    public void onStateTransitionToAdvanceTrackIfTrackEndedWithNotFoundErrorAndNotUserTriggered() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(), false));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND, trackUrn));
        verify(playQueueManager).autoNextTrack();
        verify(playbackOperations).playCurrent();
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceTrackIfTrackEndedWithNotFoundErrorAndUserTriggered() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(), true));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND, trackUrn));
        verify(playQueueManager, never()).autoNextTrack();
    }

    @Test
    public void onStateTransitionToAdvanceTrackIfTrackEndedWithForbiddenErrorAndNotUserTriggered() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(), false));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_FORBIDDEN, trackUrn));
        verify(playQueueManager).autoNextTrack();
        verify(playbackOperations).playCurrent();
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceTrackIfTrackEndedWithForbiddenErrorAndUserTriggered() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(), true));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_FORBIDDEN, trackUrn));
        verify(playQueueManager, never()).autoNextTrack();
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceTrackIfTrackEndedWithFailedErrorAndNotUserTriggered() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(), false));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_FAILED, trackUrn));
        verify(playQueueManager, never()).autoNextTrack();
    }

    @Test
    public void onStateTransitionTriesToAdvanceTrackIfTrackEndedWhileCasting() {
        when(castConnectionHelper.isCasting()).thenReturn(true);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.TRACK_COMPLETE, trackUrn));
        verify(playQueueManager).autoNextTrack();
        verify(playbackOperations, never()).playCurrent();
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
        final List<Playa.StateTransition> stateTransitionEvents = eventBus.eventsOn(EventQueue.PLAYBACK_STATE_CHANGED);
        assertThat(Iterables.filter(stateTransitionEvents, new Predicate<Playa.StateTransition>() {
            @Override
            public boolean apply(Playa.StateTransition event) {
                return event.getNewState() == Playa.PlayaState.IDLE && event.getReason() == Playa.Reason.PLAY_QUEUE_COMPLETE;
            }
        })).hasSize(1);
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

    @Test
    public void appendsRecommendedTracksWhenAtTolerance() {
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(trackUrn, Urn.NOT_SET, 0));

        verify(playQueueManager).appendUniquePlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void appendsRecommendedTracksWhenAtEnd() {
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(trackUrn, Urn.NOT_SET, 0));

        verify(playQueueManager).appendUniquePlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void doesNotAppendsRecommendedTracksWhenAtEndIfPreferenceOff() {
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);
        when(sharedPreferences.getBoolean(SettingKey.AUTOPLAY_RELATED_ENABLED, true)).thenReturn(false);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(trackUrn, Urn.NOT_SET, 0));

        verifyZeroInteractions(playQueueOperations);
    }

    @Test
    public void appendsRecommendedTracksWhenAtEndForExplore() {
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(new PlaySessionSource(Screen.EXPLORE_AUDIO_GENRE));
        when(playQueueOperations.relatedTracksPlayQueue(trackUrn, false)).thenReturn(Observable.just(recommendedPlayQueue));
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(trackUrn, Urn.NOT_SET, 0));

        verify(playQueueManager).appendUniquePlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void appendsRecommendedTracksWhenAtEndForDeeplinks() {
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(new PlaySessionSource(Screen.DEEPLINK));
        when(playQueueOperations.relatedTracksPlayQueue(trackUrn, false)).thenReturn(Observable.just(recommendedPlayQueue));
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(trackUrn, Urn.NOT_SET, 0));

        verify(playQueueManager).appendUniquePlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void appendsRecommendedTracksWhenAtEndForSearchSuggestions() {
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(new PlaySessionSource(Screen.SEARCH_SUGGESTIONS));
        when(playQueueOperations.relatedTracksPlayQueue(trackUrn, false)).thenReturn(Observable.just(recommendedPlayQueue));
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(trackUrn, Urn.NOT_SET, 0));

        verify(playQueueManager).appendUniquePlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void doesNotAppendRecommendedTracksWhenQueueIsEmpty() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(trackUrn, Urn.NOT_SET, 0));

        verifyZeroInteractions(playQueueOperations);
    }

    @Test
    public void doesNotAppendRecommendedTracksMoreThanTolerance() {
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE + 1);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(trackUrn, Urn.NOT_SET, 0));

        verifyZeroInteractions(playQueueOperations);
    }

    @Test
    public void retriesToAppendRecommendedTracksAfterError() {
        when(playQueueOperations.relatedTracksPlayQueue(trackUrn, true))
                .thenReturn(Observable.<PlayQueue>error(new IOException()), Observable.just(recommendedPlayQueue));
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(trackUrn, Urn.NOT_SET, 0));
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(trackUrn, Urn.NOT_SET, 0));

        verify(playQueueManager).appendUniquePlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void appendsRecommendedTracksConsecutivelyIfResultsAreReceivedFirstTime() {
        final Observable<PlayQueue> first = Observable.just(PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L), PlaySessionSource.EMPTY));
        when(playQueueOperations.relatedTracksPlayQueue(trackUrn, true)).thenReturn(first, Observable.just(recommendedPlayQueue));
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(trackUrn, Urn.NOT_SET, 0));
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(trackUrn, Urn.NOT_SET, 0));

        verify(playQueueManager).appendUniquePlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void doesNotAppendRecommendedTracksConsecutivelyIfNoResultsAreReceivedFirstTime() {
        when(playQueueOperations.relatedTracksPlayQueue(trackUrn, true)).thenReturn(Observable.just(PlayQueue.empty()), Observable.just(recommendedPlayQueue));
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(trackUrn, Urn.NOT_SET, 0));
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(trackUrn, Urn.NOT_SET, 0));

        verify(playQueueManager, never()).appendUniquePlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void appendsRecommendedTracksConsecutivelyIfNoResultsAreReceivedFirstTimeAndPlayQueueChanges() {
        when(playQueueOperations.relatedTracksPlayQueue(trackUrn, true)).thenReturn(Observable.just(PlayQueue.empty()), Observable.just(recommendedPlayQueue));
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(trackUrn, Urn.NOT_SET, 0));
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(Urn.NOT_SET));
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(trackUrn, Urn.NOT_SET, 0));

        verify(playQueueManager).appendUniquePlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void unsubscribesFromRecommendedTracksLoadWhenQueueChanges() {
        final PublishSubject<PlayQueue> recommendedSubject = PublishSubject.create();
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);
        when(playQueueOperations.relatedTracksPlayQueue(trackUrn, true)).thenReturn(recommendedSubject);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(trackUrn, Urn.NOT_SET, 0));

        assertThat(recommendedSubject.hasObservers()).isTrue();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(Urn.NOT_SET));

        assertThat(recommendedSubject.hasObservers()).isFalse();
    }

    private PropertySet setupTrackLoad(Urn urn) {
        PropertySet track = PropertySet.from(TrackProperty.URN.bind(urn));
        when(trackRepository.track(urn)).thenReturn(Observable.just(track));
        return track;
    }
}