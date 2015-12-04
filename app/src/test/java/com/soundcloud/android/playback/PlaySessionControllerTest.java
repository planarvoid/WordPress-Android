package com.soundcloud.android.playback;

import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedTrackForPlayer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.settings.SettingKey;
import com.soundcloud.android.stations.StationTrack;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.TestUrns;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.DisplayMetricsStub;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlaySessionControllerTest extends AndroidUnitTest {
    private final PlayQueue recommendedPlayQueue = PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L), PlaySessionSource.EMPTY);
    private final Urn LAST_URN = Urn.forTrack(987L);

    private PlayQueueItem trackPlayQueueItem;
    private Urn trackUrn;
    private PropertySet track;
    private Bitmap bitmap;
    private DisplayMetrics displayMetrics = new DisplayMetricsStub();
    private TestEventBus eventBus = new TestEventBus();

    @Mock private PlayQueueOperations playQueueOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private Resources resources;
    @Mock private TrackRepository trackRepository;
    @Mock private IRemoteAudioManager audioManager;
    @Mock private ImageOperations imageOperations;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private AdsOperations adsOperations;
    @Mock private AdsController adsController;
    @Mock private CastConnectionHelper castConnectionHelper;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private NetworkConnectionHelper networkConnectionHelper;
    @Mock private PlaybackStrategy playbackStrategy;
    @Mock private PlaybackToastHelper playbackToastHelper;
    @Mock private AccountOperations accountOperations;
    @Mock private StationsOperations stationsOperations;

    private PlaySessionController controller;
    private PublishSubject<Boolean> nextSubject = PublishSubject.create();
    private PublishSubject<Boolean> previousSubject = PublishSubject.create();

    @Before
    public void setUp() throws Exception {
        bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);

        controller = new PlaySessionController(resources, eventBus, adsOperations, adsController, playQueueManager, trackRepository,
                InjectionSupport.lazyOf(audioManager), playQueueOperations, imageOperations, playSessionStateProvider, castConnectionHelper,
                sharedPreferences, networkConnectionHelper, InjectionSupport.providerOf(playbackStrategy), playbackToastHelper, accountOperations, stationsOperations);
        controller.subscribe();

        track = expectedTrackForPlayer().put(AdProperty.IS_AUDIO_AD, false);
        trackUrn = track.get(TrackProperty.URN);
        trackPlayQueueItem = TestPlayQueueItem.createTrack(trackUrn);

        when(trackRepository.track(trackUrn)).thenReturn(Observable.just(track));
        when(playQueueManager.getLastPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(LAST_URN));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo("origin screen", true));
        when(playQueueManager.getCollectionUrn()).thenReturn(Urn.NOT_SET);
        when(playQueueOperations.relatedTracksPlayQueue(LAST_URN, true)).thenReturn(Observable.just(recommendedPlayQueue));
        when(sharedPreferences.getBoolean(SettingKey.AUTOPLAY_RELATED_ENABLED, true)).thenReturn(true);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(PlaySessionSource.EMPTY);
        when(resources.getDisplayMetrics()).thenReturn(displayMetrics);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(456L));
        when(playbackStrategy.playCurrent()).thenReturn(Observable.<Void>just(null));
        when(playQueueManager.moveToNextPlayableItem(anyBoolean())).thenReturn(nextSubject);
        when(playQueueManager.moveToPreviousPlayableItem(anyBoolean())).thenReturn(previousSubject);
    }

    @Test
    public void playQueueTrackChangedHandlerCallsPlayCurrentIfThePlayerIsInPlaySession() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playbackStrategy).playCurrent();
    }

    @Test
    public void playQueueTrackChangedHandlerCallsPlayCurrentAndPausesIfNextTrackBlocked() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        when(playbackStrategy.playCurrent()).thenReturn(Observable.<Void>error(new BlockedTrackException(trackUrn)));

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playbackStrategy).pause();
    }

    @Test
    public void playQueueTrackChangedHandlerDoesNotCallPlayCurrentIfPlaySessionIsNotActive() {
        final Player.StateTransition lastTransition = Mockito.mock(Player.StateTransition.class);
        when(lastTransition.playSessionIsActive()).thenReturn(false);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, lastTransition);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playbackStrategy, never()).playCurrent();
    }

    @Test
    public void playQueueTrackChangeWhenCastingPlaysTrackWhenCurrentTrackIsDifferentAndPlaying() {
        when(castConnectionHelper.isCasting()).thenReturn(true);
        when(playSessionStateProvider.isPlaying()).thenReturn(true);

        final PropertySet previousCurrentTrack = setupTrackLoad(Urn.forTrack(5L));
        final PropertySet newCurrentTrack = setupTrackLoad(Urn.forTrack(6L));
        final PlayQueueItem previousPlayQueueItem = TestPlayQueueItem.createTrack(previousCurrentTrack.get(TrackProperty.URN));
        final PlayQueueItem newPlayQueueItem = TestPlayQueueItem.createTrack(newCurrentTrack.get(TrackProperty.URN));

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(previousPlayQueueItem, Urn.NOT_SET, 0));
        Mockito.reset(playbackStrategy);
        when(playbackStrategy.playCurrent()).thenReturn(Observable.<Void>just(null));
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(newPlayQueueItem, Urn.NOT_SET, 0));

        verify(playbackStrategy).playCurrent();
    }

    @Test
    public void playQueueTrackChangeWhenCastingDoesNotPlayTrackWhenCurrentTrackStaysTheSame() {
        when(castConnectionHelper.isCasting()).thenReturn(true);
        when(playSessionStateProvider.isPlaying()).thenReturn(true);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));
        Mockito.reset(playbackStrategy);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playbackStrategy, never()).playCurrent();
    }

    @Test
    public void playQueueTrackChangedHandlerDoesNotSetTrackOnAudioManagerIfTrackChangeNotSupported() {
        when(audioManager.isTrackChangeSupported()).thenReturn(false);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));
        verify(audioManager, never()).onTrackChanged(any(PropertySet.class), any(Bitmap.class));
    }

    @Test
    public void playQueueChangedHandlerSetsLockScreenStateWithBitmapForCurrentTrack() {
        when(audioManager.isTrackChangeSupported()).thenReturn(true);
        when(imageOperations.artwork(trackUrn, ApiImageSize.T500)).thenReturn(Observable.just(bitmap));

        InOrder inOrder = Mockito.inOrder(audioManager);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));
        inOrder.verify(audioManager).onTrackChanged(track, null);
        inOrder.verify(audioManager).onTrackChanged(eq(track), any(Bitmap.class));
    }

    @Test
    public void playQueueChangedHandlerSetsLockScreenStateWithBitmapForCurrentAudioAdTrack() {
        when(audioManager.isTrackChangeSupported()).thenReturn(true);
        when(imageOperations.artwork(trackUrn, ApiImageSize.T500)).thenReturn(Observable.just(bitmap));

        InOrder inOrder = Mockito.inOrder(audioManager);
        trackPlayQueueItem = TestPlayQueueItem.createTrack(trackUrn, AdFixtures.getAudioAd(Urn.forTrack(123L)));
        track.put(AdProperty.IS_AUDIO_AD, true);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));
        inOrder.verify(audioManager).onTrackChanged(eq(track), eq(((Bitmap) null)));
        inOrder.verify(audioManager).onTrackChanged(eq(track), any(Bitmap.class));
    }

    @Test
    public void playQueueTrackChangedHandlerSetsLockScreenStateWithNullBitmapForCurrentTrackOnImageLoadError() {
        when(audioManager.isTrackChangeSupported()).thenReturn(true);
        when(imageOperations.artwork(trackUrn, ApiImageSize.T500)).thenReturn(Observable.<Bitmap>error(new Exception("Could not load image")));

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));
        verify(audioManager).onTrackChanged(track, null);
    }

    @Test
    public void shouldNotRespondToPlayQueueTrackChangesWhenPlayerIsNotPlaying() {
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playbackStrategy, never()).playCurrent();
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceItemIfTrackNotEnded() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.PLAYING, Player.Reason.NONE, trackUrn));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.BUFFERING, Player.Reason.NONE, trackUrn));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_FAILED, trackUrn));
        assertThat(nextSubject.hasObservers()).isFalse();
    }

    @Test
    public void onStateTransitionDoesNotAdvanceItemIfTrackEndedWithNotFoundErrorAndNotUserTriggeredWithNoConnection() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(), false));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_NOT_FOUND, trackUrn));
        assertThat(nextSubject.hasObservers()).isFalse();
    }

    @Test
    public void onStateTransitionToAdvanceItemIfTrackEndedWithNotFoundErrorAndNotUserTriggeredWithConnection() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(), false));
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_NOT_FOUND, trackUrn));
        nextSubject.onNext(true);
        verify(playbackStrategy).playCurrent();
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceItemIfTrackEndedWithNotFoundErrorAndUserTriggered() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(), true));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_NOT_FOUND, trackUrn));
        assertThat(nextSubject.hasObservers()).isFalse();
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceItemIfTrackEndedWithForbiddenErrorAndNotUserTriggeredAndNoInternet() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(), false));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_FORBIDDEN, trackUrn));
        assertThat(nextSubject.hasObservers()).isFalse();
    }

    @Test
    public void onStateTransitionToAdvanceItemIfTrackEndedWithForbiddenErrorAndNotUserTriggeredWithConnection() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(), false));
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_FORBIDDEN, trackUrn));
        nextSubject.onNext(true);
        verify(playbackStrategy).playCurrent();
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceItemIfTrackEndedWithForbiddenErrorAndUserTriggered() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(), true));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_FORBIDDEN, trackUrn));
        assertThat(nextSubject.hasObservers()).isFalse();
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceItemIfTrackEndedWithFailedErrorAndNotUserTriggered() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(), false));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_FAILED, trackUrn));
        assertThat(nextSubject.hasObservers()).isFalse();
    }

    @Test
    public void onStateTransitionTriesToAdvanceItemIfTrackEndedWhileCasting() {
        when(castConnectionHelper.isCasting()).thenReturn(true);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.TRACK_COMPLETE, trackUrn));
        assertThat(nextSubject.hasObservers()).isTrue();
        nextSubject.onNext(false);
        verify(playbackStrategy, never()).playCurrent();
    }

    @Test
    public void onStateTransitionTriesToReconfigureAd() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.TRACK_COMPLETE, trackUrn));
        verify(adsController).reconfigureAdForNextTrack();
    }

    @Test
    public void onStateTransitionDoesNotOpenCurrentTrackAfterFailingToAdvancePlayQueue() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.TRACK_COMPLETE, trackUrn));
        assertThat(nextSubject.hasObservers()).isTrue();
        nextSubject.onNext(false);
        verifyZeroInteractions(playbackStrategy);
    }

    @Test
    public void onStateTransitionPublishesPlayQueueCompleteEventAfterFailingToAdvancePlayQueue() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.TRACK_COMPLETE, trackUrn));
        assertThat(nextSubject.hasObservers()).isTrue();
        nextSubject.onNext(false);
        final List<Player.StateTransition> stateTransitionEvents = eventBus.eventsOn(EventQueue.PLAYBACK_STATE_CHANGED);
        assertThat(Iterables.filter(stateTransitionEvents, new Predicate<Player.StateTransition>() {
            @Override
            public boolean apply(Player.StateTransition event) {
                return event.getNewState() == Player.PlayerState.IDLE && event.getReason() == Player.Reason.PLAY_QUEUE_COMPLETE;
            }
        })).hasSize(1);
    }

    @Test
    public void onStateTransitionForQueueCompleteDoesNotSavePosition() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.PLAY_QUEUE_COMPLETE, trackUrn));
        verify(playQueueManager, never()).saveCurrentProgress(anyInt());
    }

    @Test
    public void onStateTransitionForBufferingDoesNotSaveQueuePosition() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.BUFFERING, Player.Reason.NONE, trackUrn, 123, 456));
        verify(playQueueManager, never()).saveCurrentProgress(anyInt());
    }

    @Test
    public void onStateTransitionForPlayingDoesNotSaveQueuePosition() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.PLAYING, Player.Reason.NONE, trackUrn, 123, 456));
        verify(playQueueManager, never()).saveCurrentProgress(anyInt());
    }

    @Test
    public void appendsRecommendedTracksWhenAtTolerance() {
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playQueueManager).appendUniquePlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void appendsRecommendedTracksWhenAtEnd() {
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playQueueManager).appendUniquePlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void appendsStationsTracksWhenAtTheEndOfAStationsPlayQueue() {
        final Urn station = Urn.forTrackStation(123L);
        final PlayQueue playQueue = PlayQueue.fromStation(station, Collections.singletonList(StationTrack.create(trackUrn, Urn.NOT_SET)));
        final int queueSize = PlaySessionController.RECOMMENDED_LOAD_TOLERANCE;

        when(playQueueManager.getQueueSize()).thenReturn(queueSize);
        when(playQueueManager.getCollectionUrn()).thenReturn(station);
        when(stationsOperations.fetchUpcomingTracks(station, queueSize)).thenReturn(Observable.just(playQueue));

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, station, 0));

        verify(playQueueManager).appendPlayQueueItems(playQueue);
    }

    @Test
    public void doesNotAppendsRecommendedTracksWhenAtEndIfPreferenceOff() {
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);
        when(sharedPreferences.getBoolean(SettingKey.AUTOPLAY_RELATED_ENABLED, true)).thenReturn(false);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verifyZeroInteractions(playQueueOperations);
    }

    @Test
    public void appendsRecommendedTracksWhenAtEndForExplore() {
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(new PlaySessionSource(Screen.EXPLORE_AUDIO_GENRE));
        when(playQueueOperations.relatedTracksPlayQueue(LAST_URN, false)).thenReturn(Observable.just(recommendedPlayQueue));
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playQueueManager).appendUniquePlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void appendsRecommendedTracksWhenAtEndForDeeplinks() {
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(new PlaySessionSource(Screen.DEEPLINK));
        when(playQueueOperations.relatedTracksPlayQueue(LAST_URN, false)).thenReturn(Observable.just(recommendedPlayQueue));
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playQueueManager).appendUniquePlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void appendsRecommendedTracksWhenAtEndForSearchSuggestions() {
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(new PlaySessionSource(Screen.SEARCH_SUGGESTIONS));
        when(playQueueManager.isQueueEmpty()).thenReturn(false);
        when(playQueueOperations.relatedTracksPlayQueue(LAST_URN, false)).thenReturn(Observable.just(recommendedPlayQueue));
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playQueueManager).appendUniquePlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void doesNotAppendRecommendedTracksWhenQueueIsEmpty() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verifyZeroInteractions(playQueueOperations);
    }

    @Test
    public void doesNotAppendRecommendedTracksMoreThanTolerance() {
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE + 1);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verifyZeroInteractions(playQueueOperations);
    }

    @Test
    public void retriesToAppendRecommendedTracksAfterError() {
        when(playQueueOperations.relatedTracksPlayQueue(LAST_URN, true))
                .thenReturn(Observable.<PlayQueue>error(new IOException()), Observable.just(recommendedPlayQueue));
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playQueueManager).appendUniquePlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void appendsRecommendedTracksConsecutivelyIfResultsAreReceivedFirstTime() {
        final Observable<PlayQueue> first = Observable.just(PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L), PlaySessionSource.EMPTY));
        when(playQueueManager.isQueueEmpty()).thenReturn(false);
        when(playQueueOperations.relatedTracksPlayQueue(LAST_URN, true)).thenReturn(first, Observable.just(recommendedPlayQueue));
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playQueueManager).appendUniquePlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void doesNotAppendRecommendedTracksConsecutivelyIfNoResultsAreReceivedFirstTime() {
        when(playQueueOperations.relatedTracksPlayQueue(LAST_URN, true)).thenReturn(Observable.just(PlayQueue.empty()), Observable.just(recommendedPlayQueue));
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playQueueManager, never()).appendUniquePlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void appendsRecommendedTracksConsecutivelyIfNoResultsAreReceivedFirstTimeAndPlayQueueChanges() {
        when(playQueueManager.isQueueEmpty()).thenReturn(false);
        when(playQueueOperations.relatedTracksPlayQueue(LAST_URN, true)).thenReturn(Observable.just(PlayQueue.empty()), Observable.just(recommendedPlayQueue));
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(Urn.NOT_SET));
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playQueueManager).appendUniquePlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void unsubscribesFromRecommendedTracksLoadWhenQueueChanges() {
        final PublishSubject<PlayQueue> recommendedSubject = PublishSubject.create();
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);
        when(playQueueOperations.relatedTracksPlayQueue(LAST_URN, true)).thenReturn(recommendedSubject);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        assertThat(recommendedSubject.hasObservers()).isTrue();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(Urn.NOT_SET));

        assertThat(recommendedSubject.hasObservers()).isFalse();
    }

    @Test
    public void togglePlaybackShouldTogglePlaybackOnPlaybackStrategyIfPlayingCurrentTrack() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueTrack()).thenReturn(true);
        controller.togglePlayback();

        verify(playbackStrategy).togglePlayback();
    }

    @Test
    public void togglePlaybackShouldNotTogglePlaybackOnPlaybackStrategyIfNotPlayingCurrentTrack() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueTrack()).thenReturn(false);
        controller.togglePlayback();

        verify(playbackStrategy, never()).togglePlayback();
    }

    @Test
    public void playCurrentCallsPlayCurrentOnPlaybackStrategy() {
        controller.playCurrent();

        verify(playbackStrategy).playCurrent();
    }

    @Test
    public void playCurrentWhenEmptyCallsLoadsQueueBeforePlayingCurrentOnPlaybackStrategy() {
        final PublishSubject<PlayQueue> subject = PublishSubject.create();
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        when(playQueueManager.loadPlayQueueAsync()).thenReturn(subject);

        controller.playCurrent();

        verify(playbackStrategy, never()).playCurrent();

        subject.onNext(PlayQueue.empty());

        verify(playbackStrategy).playCurrent();
    }

    @Test
    public void shouldUpdatePlayPositionToGivenIndex() {
        controller.setPlayQueuePosition(5);

        verify(playQueueManager).setPosition(5, true);
    }

    @Test
    public void settingPlayQueuePositionPublishesAdSkippedTrackingEventWhenTrackIsAudioAd() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS + 1);

        controller.setPlayQueuePosition(5);

        // make sure we test for the current track being an ad *before* we skip
        InOrder inOrder = inOrder(adsOperations, playQueueManager);
        inOrder.verify(adsOperations, atLeastOnce()).isCurrentItemAudioAd();
        inOrder.verify(playQueueManager).setPosition(5, true);

        final UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(UIEvent.KIND_SKIP_AUDIO_AD_CLICK);
        assertThat(event.getAttributes().get("ad_track_urn")).isEqualTo(Urn.forTrack(123).toString());
    }

    @Test
    public void settingPlayQueuePositionDoesNotPublishAdSkippedTrackingEventWhenTrackNotAnAd() {
        controller.setPlayQueuePosition(5);

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void previousTrackCallsMoveToPreviousTrackOnPlayQueueManagerIfProgressLessThanTolerance() {
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(2999L, 5000));

        controller.previousTrack();

        assertThat(previousSubject.hasObservers()).isTrue();
    }

    @Test
    public void previousTrackCallsMoveToPreviousTrackOnPlayQueueManagerIfProgressEqualToleranceAndPlayingAudioAd() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(3000L, 5000));

        controller.previousTrack();

        assertThat(previousSubject.hasObservers()).isTrue();
    }

    @Test
    public void previousTrackCallsUnsubscribesFromNextTrackCall() {
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(0, 5000));

        controller.previousTrack();

        assertThat(previousSubject.hasObservers()).isTrue();

        controller.nextTrack();

        assertThat(previousSubject.hasObservers()).isFalse();
    }

    @Test
    public void nextTrackCallsUnsubscribesFromPreviousTrackCall() {
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(0, 5000));

        controller.nextTrack();

        assertThat(nextSubject.hasObservers()).isTrue();

        controller.previousTrack();

        assertThat(nextSubject.hasObservers()).isFalse();
    }

    @Test
    public void previousTrackSeeksToZeroIfProgressEqualToTolerance() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueTrack()).thenReturn(true);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(3000L, 5000));

        controller.previousTrack();

        verify(playbackStrategy).seek(0);
    }

    @Test
    public void previousTrackSeeksToZeroIfProgressGreaterThanTolerance() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueTrack()).thenReturn(true);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(3001L, 5000));

        controller.previousTrack();

        verify(playbackStrategy).seek(0);
    }

    @Test
    public void previousTrackCallsPreviousItemIfPlayingAudioAdWithProgressEqualToTimeout() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(PlaybackProgress.empty());

        controller.previousTrack();

        assertThat(previousSubject.hasObservers()).isTrue();
    }

    @Test
    public void previousTrackDoesNothingIfPlayingAudioAdWithProgressLessThanTimeout() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        controller.previousTrack();

        assertThat(previousSubject.hasObservers()).isFalse();
    }

    @Test
    public void previousTrackShowsUnskippableToastWhenPlaybackNotSkippable() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        controller.previousTrack();

        verify(playbackToastHelper).showUnskippableAdToast();
    }

    @Test
    public void previousTrackPublishesAdSkippedTrackingEventWhenTrackIsAudioAd() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS + 1);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(3000L, 5000));

        controller.previousTrack();

        // make sure we test for the current track being an ad *before* we skip
        verify(adsOperations, atLeastOnce()).isCurrentItemAudioAd();
        assertThat(previousSubject.hasObservers()).isTrue();

        final UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(UIEvent.KIND_SKIP_AUDIO_AD_CLICK);
        assertThat(event.getAttributes().get("ad_track_urn")).isEqualTo(Urn.forTrack(123).toString());
    }

    @Test
    public void previousTrackDoesNotPublishAdSkippedTrackingEventWhenAdNotYetSkippable() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        controller.previousTrack();

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void previousTrackDoesNotPublishAdSkippedTrackingEventWhenTrackNotAnAd() {
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(3000L, 5000));

        controller.previousTrack();

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void nextTrackShowsUnskippableToastWhenPlaybackNotSkippable() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        controller.nextTrack();

        verify(playbackToastHelper).showUnskippableAdToast();
    }

    @Test
    public void nextTrackCallsNextItemOnPlayQueueManager() {
        controller.nextTrack();

        assertThat(nextSubject.hasObservers()).isTrue();
    }

    @Test
    public void nextTrackCallsNextItemIfPlayingAudioAdWithProgressEqualToTimeout() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS);

        controller.nextTrack();

        assertThat(nextSubject.hasObservers()).isTrue();
    }

    @Test
    public void nextTrackDoesNothingIfPlayingAudioAdWithProgressLessThanTimeout() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        controller.nextTrack();

        assertThat(nextSubject.hasObservers()).isFalse();
    }

    @Test
    public void nextTrackPublishesAdSkippedTrackingEventWhenTrackIsAudioAd() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS + 1);

        controller.nextTrack();

        // make sure we test for the current track being an ad *before* we skip
        verify(adsOperations, atLeastOnce()).isCurrentItemAudioAd();
        assertThat(nextSubject.hasObservers()).isTrue();

        final UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(UIEvent.KIND_SKIP_AUDIO_AD_CLICK);
        assertThat(event.getAttributes().get("ad_track_urn")).isEqualTo(Urn.forTrack(123).toString());
    }

    @Test
    public void nextTrackDoesNotPublishAdSkippedTrackingEventWhenAdNotYetSkippable() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        controller.nextTrack();

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void nextTrackDoesNotPublishAdSkippedTrackingEventWhenTrackNotAnAd() {
        controller.nextTrack();

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void seeksToProvidedPositionIfServiceIsPlayingCurrentTrack() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueTrack()).thenReturn(true);
        controller.seek(350L);

        verify(playbackStrategy).seek(350L);
    }

    @Test
    public void seeksSavesPlayQueueProgressToSeekPositionIfNotPlayingCurrentTrack() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueTrack()).thenReturn(false);
        controller.seek(350L);

        verify(playQueueManager).saveCurrentProgress(350L);
    }

    @Test
    public void seekSeeksToProvidedPositionIfPlayingAudioAdWithProgressEqualTimeout() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueTrack()).thenReturn(true);
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS);

        controller.seek(350L);

        verify(playbackStrategy).seek(350L);
    }

    @Test
    public void seekDoesNothingIfPlayingAudioAdWithProgressLessThanTimeout() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        controller.seek(350L);

        verifyZeroInteractions(playbackStrategy);
    }

    @Test
    public void playNewQueueWhenSkippablePlaysQueue() {
        Urn track = Urn.forTrack(123L);

        final TestSubscriber<PlaybackResult> subscriber = new TestSubscriber<>();
        final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.ACTIVITIES);
        final PlayQueue playQueue = PlayQueue.fromTrackUrnList(Arrays.asList(track), playSessionSource);
        setupSetNewQueue(track, playSessionSource, playQueue, Observable.just(PlaybackResult.success()));

        controller.playNewQueue(playQueue, track, 0, playSessionSource)
                .subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).isSuccess()).isTrue();
    }

    @Test
    public void playNewQueueWhenUnskippableReturnsPlaybackError() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);
        Urn track = Urn.forTrack(123L);

        final TestSubscriber<PlaybackResult> subscriber = new TestSubscriber<>();
        controller.playNewQueue(PlayQueue.fromTrackUrnList(Arrays.asList(track), PlaySessionSource.EMPTY), track, 0, PlaySessionSource.EMPTY)
                .subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).isSuccess()).isFalse();
        assertThat(subscriber.getOnNextEvents().get(0).getErrorReason()).isEqualTo(PlaybackResult.ErrorReason.UNSKIPPABLE);
    }

    @Test
    public void playNewQueueDoesNotPlayCurrentTrackIfErrorSettingQueue() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);
        Urn track = Urn.forTrack(123L);
        final PublishSubject<Void> playCurrentSubject = PublishSubject.create();
        when(playbackStrategy.playCurrent()).thenReturn(playCurrentSubject);

        final TestSubscriber<PlaybackResult> subscriber = new TestSubscriber<>();
        controller.playNewQueue(PlayQueue.fromTrackUrnList(Arrays.asList(track), PlaySessionSource.EMPTY), track, 0, PlaySessionSource.EMPTY)
                .subscribe(subscriber);

        assertThat(playCurrentSubject.hasObservers()).isFalse();
    }

    @Test
    public void playNewQueueUnsubscribesFromCurrentTrackLoad() {
        Urn track = Urn.forTrack(123L);

        final PublishSubject<Void> playCurrentSubject = PublishSubject.create();
        when(playbackStrategy.playCurrent()).thenReturn(playCurrentSubject);

        final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.ACTIVITIES);
        final PlayQueue playQueue = PlayQueue.fromTrackUrnList(Arrays.asList(track), playSessionSource);
        setupSetNewQueue(track, playSessionSource, playQueue, Observable.<PlaybackResult>never());

        controller.playCurrent();

        assertThat(playCurrentSubject.hasObservers()).isTrue();

        controller.playNewQueue(playQueue, track, 0, playSessionSource).subscribe(new TestSubscriber<PlaybackResult>());

        assertThat(playCurrentSubject.hasObservers()).isFalse();
    }

    @Test
    public void playCurrentUnsubscribesFromPreviousTrackLoad() {
        final PublishSubject<Void> playCurrentSubject = PublishSubject.create();
        when(playbackStrategy.playCurrent()).thenReturn(playCurrentSubject, Observable.<Void>never());

        controller.playCurrent();

        assertThat(playCurrentSubject.hasObservers()).isTrue();

        controller.playCurrent();

        assertThat(playCurrentSubject.hasObservers()).isFalse();

    }

    @Test
    public void playNewQueuePlaysCurrentTrackAfterSettingQueue() {
        Urn track = Urn.forTrack(123L);
        final PublishSubject<Void> playCurrentSubject = PublishSubject.create();
        when(playbackStrategy.playCurrent()).thenReturn(playCurrentSubject);

        final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.ACTIVITIES);
        final PlayQueue playQueue = PlayQueue.fromTrackUrnList(Arrays.asList(track), playSessionSource);
        setupSetNewQueue(track, playSessionSource, playQueue, Observable.just(PlaybackResult.success()));

        final TestSubscriber<PlaybackResult> subscriber = new TestSubscriber<>();
        controller.playNewQueue(playQueue, track, 0, playSessionSource)
                .subscribe(subscriber);

        assertThat(playCurrentSubject.hasObservers()).isTrue();
    }

    @Test
    public void reloadPlayQueueIfEmptyDoesNotReloadQueueIfQueueNotEmpty() {
        final PublishSubject<PlayQueue> subject = PublishSubject.create();
        when(playQueueManager.loadPlayQueueAsync()).thenReturn(subject);

        controller.reloadQueueAndShowPlayerIfEmpty();

        assertThat(subject.hasObservers()).isFalse();
    }

    @Test
    public void reloadPlayQueueReloadsIfQueueEmpty() {
        final PublishSubject<PlayQueue> subject = PublishSubject.create();
        when(playQueueManager.loadPlayQueueAsync()).thenReturn(subject);
        when(playQueueManager.isQueueEmpty()).thenReturn(true);

        controller.reloadQueueAndShowPlayerIfEmpty();

        assertThat(subject.hasObservers()).isTrue();

    }

    private void setupSetNewQueue(Urn track, PlaySessionSource playSessionSource, PlayQueue playQueue, Observable<PlaybackResult> result) {
        when(playbackStrategy.setNewQueue(playQueue, track, 0, playSessionSource))
                .thenReturn(result);
    }

    private void setupAdInProgress(long currentProgress) {
        final PlaybackProgress progress = new PlaybackProgress(currentProgress, 30000);
        final AudioAd adData = AdFixtures.getAudioAd(Urn.forTrack(456L));
        final PlayQueueItem playQueueItem = TestPlayQueueItem.createTrack(Urn.forTrack(123L), adData);

        when(playSessionStateProvider.getLastProgressEventForCurrentPlayQueueTrack()).thenReturn(progress);
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(playQueueItem);
    }

    private PropertySet setupTrackLoad(Urn urn) {
        PropertySet track = PropertySet.from(TrackProperty.URN.bind(urn));
        when(trackRepository.track(urn)).thenReturn(Observable.just(track));
        return track;
    }
}
