package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.PlaybackServiceInitiator;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.settings.SettingKey;
import com.soundcloud.android.stations.StationTrack;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.TestUrns;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueue;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import android.content.SharedPreferences;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlaySessionControllerTest extends AndroidUnitTest {
    private final PlayQueue recommendedPlayQueue = TestPlayQueue.fromUrns(TestUrns.createTrackUrns(1L, 2L), PlaySessionSource.EMPTY);
    private final Urn LAST_URN = Urn.forTrack(987L);

    private PlayQueueItem trackPlayQueueItem;
    private Urn trackUrn;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private PlayQueueOperations playQueueOperations;
    @Mock private PlayQueueManager playQueueManager;
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
    @Mock private FeatureFlags featureFlags;
    @Mock private PlaybackServiceInitiator playbackServiceInitiator;

    private PlaySessionController controller;
    private PublishSubject<Void> playCurrentSubject;

    @Before
    public void setUp() throws Exception {
        controller = new PlaySessionController(eventBus, adsOperations, adsController, playQueueManager,
                playQueueOperations, playSessionStateProvider, castConnectionHelper,
                sharedPreferences, networkConnectionHelper, InjectionSupport.providerOf(playbackStrategy), playbackToastHelper, accountOperations, stationsOperations, playbackServiceInitiator);
        controller.subscribe();

        trackUrn = Urn.forTrack(123);
        trackPlayQueueItem = TestPlayQueueItem.createTrack(trackUrn);
        playCurrentSubject = PublishSubject.create();

        when(playQueueManager.getLastPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(LAST_URN));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(playQueueManager.isCurrentTrack(trackUrn)).thenReturn(true);
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo("origin screen", true));
        when(playQueueManager.getCollectionUrn()).thenReturn(Urn.NOT_SET);
        when(playQueueOperations.relatedTracksPlayQueue(LAST_URN, true)).thenReturn(Observable.just(recommendedPlayQueue));
        when(sharedPreferences.getBoolean(SettingKey.AUTOPLAY_RELATED_ENABLED, true)).thenReturn(true);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(PlaySessionSource.EMPTY);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(456L));
        when(playbackStrategy.playCurrent()).thenReturn(playCurrentSubject);
        when(playQueueManager.getUpcomingPlayQueueItems(anyInt())).thenReturn(Lists.<Urn>newArrayList());
        when(featureFlags.isEnabled(Flag.EXPLODE_PLAYLISTS_IN_PLAYQUEUES)).thenReturn(true);
    }

    @Test
    public void playQueueTrackChangedHandlerCallsPlayCurrentIfCurrentItemIsVideoAdIfThePlayerIsInPlaySession() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        final VideoQueueItem videoItem = TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(trackUrn));

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(videoItem, Urn.NOT_SET, 0));

        assertThat(playCurrentSubject.hasObservers()).isTrue();
    }

    @Test
    public void playQueueTrackChangedHandlerCallsPlayCurrentIfThePlayerIsInPlaySession() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));

        assertThat(playCurrentSubject.hasObservers()).isTrue();
    }

    @Test
    public void playQueueTrackChangedHandlerCallsPlayCurrentAndPausesIfNextTrackBlocked() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        when(playbackStrategy.playCurrent()).thenReturn(Observable.<Void>error(new BlockedTrackException(trackUrn)));

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playbackStrategy).pause();
    }

    @Test
    public void playQueueTrackChangedHandlerDoesNotCallPlayCurrentForTrackIfPlaySessionIsNotActive() {
        final Player.StateTransition lastTransition = Mockito.mock(Player.StateTransition.class);
        when(lastTransition.playSessionIsActive()).thenReturn(false);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, lastTransition);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        assertThat(playCurrentSubject.hasObservers()).isFalse();
    }

    @Test
    public void playQueueTrackChangeHandlerDoesNotCallPlayCurrentForVideoAdIfPlaySessionIsNotActive() {
        final Player.StateTransition lastTransition = Mockito.mock(Player.StateTransition.class);
        when(lastTransition.playSessionIsActive()).thenReturn(false);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, lastTransition);

        final VideoQueueItem videoItem = TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(trackUrn));
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(videoItem, Urn.NOT_SET, 0));

        assertThat(playCurrentSubject.hasObservers()).isFalse();
    }

    @Test
    public void playQueueTrackChangeWhenCastingPlaysTrackWhenCurrentTrackIsDifferentAndPlaying() {
        when(castConnectionHelper.isCasting()).thenReturn(true);
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        when(playbackStrategy.playCurrent()).thenReturn(playCurrentSubject);

        final PlayQueueItem newPlayQueueItem = TestPlayQueueItem.createTrack(Urn.forTrack(2));
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(newPlayQueueItem, Urn.NOT_SET, 0));

        assertThat(playCurrentSubject.hasObservers()).isTrue();
    }

    @Test
    public void playQueueTrackChangeWhenCastingDoesNotPlayTrackWhenCurrentTrackStaysTheSame() {
        when(castConnectionHelper.isCasting()).thenReturn(true);
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        when(playbackStrategy.playCurrent()).thenReturn(playCurrentSubject);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));

        assertThat(playCurrentSubject.hasObservers()).isFalse();
    }

    @Test
    public void shouldNotRespondToPlayQueueTrackChangesWhenPlayerIsNotPlaying() {
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        assertThat(playCurrentSubject.hasObservers()).isFalse();
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceItemIfTrackNotEnded() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.PLAYING, Player.Reason.NONE, trackUrn));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.BUFFERING, Player.Reason.NONE, trackUrn));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_FAILED, trackUrn));
        verify(playQueueManager, never()).moveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionDoesNotAdvanceItemIfTrackEndedWithNotFoundErrorAndNotUserTriggeredWithNoConnection() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(), false));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_NOT_FOUND, trackUrn));
        verify(playQueueManager, never()).moveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionToAdvanceItemIfTrackEndedWithNotFoundErrorAndNotUserTriggeredWithConnection() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(), false));
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);
        when(playQueueManager.autoMoveToNextPlayableItem()).thenReturn(true);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_NOT_FOUND, trackUrn));
        verify(playQueueManager).autoMoveToNextPlayableItem();
        assertThat(playCurrentSubject.hasObservers()).isTrue();
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceItemIfTrackEndedWithNotFoundErrorAndUserTriggered() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(), true));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_NOT_FOUND, trackUrn));
        verify(playQueueManager, never()).moveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceItemIfTrackEndedWithForbiddenErrorAndNotUserTriggeredAndNoInternet() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(), false));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_FORBIDDEN, trackUrn));
        verify(playQueueManager, never()).moveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionToAdvanceItemIfTrackEndedWithForbiddenErrorAndNotUserTriggeredWithConnection() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(), false));
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);
        when(playQueueManager.autoMoveToNextPlayableItem()).thenReturn(true);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_FORBIDDEN, trackUrn));
        verify(playQueueManager).autoMoveToNextPlayableItem();
        assertThat(playCurrentSubject.hasObservers()).isTrue();
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceItemIfTrackEndedWithForbiddenErrorAndUserTriggered() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(), true));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_FORBIDDEN, trackUrn));
        verify(playQueueManager, never()).moveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceItemIfTrackEndedWithFailedErrorAndNotUserTriggered() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(), false));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_FAILED, trackUrn));
        verify(playQueueManager, never()).moveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionDoesNotAdvanceTracksIfNotCurrentPlayQueueTrack() {
        when(playQueueManager.isCurrentTrack(trackUrn)).thenReturn(false);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.PLAYBACK_COMPLETE, trackUrn));
        verify(playQueueManager, never()).autoMoveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionTriesToAdvanceItemIfTrackEndedWhileCasting() {
        when(castConnectionHelper.isCasting()).thenReturn(true);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.PLAYBACK_COMPLETE, trackUrn));
        verify(playQueueManager).autoMoveToNextPlayableItem();
        assertThat(playCurrentSubject.hasObservers()).isFalse();
    }

    @Test
    public void onStateTransitionTriesToAdvanceItem() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.PLAYBACK_COMPLETE, trackUrn));
        verify(playQueueManager).autoMoveToNextPlayableItem();
        assertThat(playCurrentSubject.hasObservers()).isFalse();
    }

    @Test
    public void onStateTransitionTriesToReconfigureAd() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.PLAYBACK_COMPLETE, trackUrn));
        verify(adsController).reconfigureAdForNextTrack();
        verify(adsController).publishAdDeliveryEventIfUpcoming();
    }

    @Test
    public void onStateTransitionDoesNotOpenCurrentTrackAfterFailingToAdvancePlayQueue() throws Exception {
        when(playQueueManager.moveToNextPlayableItem()).thenReturn(false);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.PLAYBACK_COMPLETE, trackUrn));
        verifyZeroInteractions(playbackStrategy);
    }

    @Test
    public void onStateTransitionPublishesPlayQueueCompleteEventAfterFailingToAdvancePlayQueue() throws Exception {
        when(playQueueManager.moveToNextPlayableItem()).thenReturn(false);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.PLAYBACK_COMPLETE, trackUrn));
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

        verify(playQueueManager).appendPlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void appendsRecommendedTracksWhenAtEnd() {
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playQueueManager).appendPlayQueueItems(recommendedPlayQueue);
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

        verify(playQueueManager).appendPlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void appendsRecommendedTracksWhenAtEndForDeeplinks() {
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(new PlaySessionSource(Screen.DEEPLINK));
        when(playQueueOperations.relatedTracksPlayQueue(LAST_URN, false)).thenReturn(Observable.just(recommendedPlayQueue));
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playQueueManager).appendPlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void appendsRecommendedTracksWhenAtEndForSearchSuggestions() {
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(new PlaySessionSource(Screen.SEARCH_SUGGESTIONS));
        when(playQueueManager.isQueueEmpty()).thenReturn(false);
        when(playQueueOperations.relatedTracksPlayQueue(LAST_URN, false)).thenReturn(Observable.just(recommendedPlayQueue));
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playQueueManager).appendPlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void doesNotAppendRecommendedTracksWhenQueueIsEmpty() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verifyZeroInteractions(playQueueOperations);
    }

    @Test
    public void doesNotAppendRecommendedTracksMoreThanTolerance() {
        when(playQueueManager.getPlayableQueueItemsRemaining()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE + 1);
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

        verify(playQueueManager).appendPlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void appendsRecommendedTracksConsecutivelyIfResultsAreReceivedFirstTime() {
        final Observable<PlayQueue> first = Observable.just(TestPlayQueue.fromUrns(TestUrns.createTrackUrns(1L), PlaySessionSource.EMPTY));
        when(playQueueManager.isQueueEmpty()).thenReturn(false);
        when(playQueueOperations.relatedTracksPlayQueue(LAST_URN, true)).thenReturn(first, Observable.just(recommendedPlayQueue));
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playQueueManager).appendPlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void doesNotAppendRecommendedTracksConsecutivelyIfNoResultsAreReceivedFirstTime() {
        when(playQueueOperations.relatedTracksPlayQueue(LAST_URN, true)).thenReturn(Observable.just(PlayQueue.empty()), Observable.just(recommendedPlayQueue));
        when(playQueueManager.getQueueSize()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlaySessionController.RECOMMENDED_LOAD_TOLERANCE - 1);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playQueueManager, never()).appendPlayQueueItems(recommendedPlayQueue);
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

        verify(playQueueManager).appendPlayQueueItems(recommendedPlayQueue);
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
    public void togglePlaybackShouldTogglePlaybackStrategyIfVideoAd() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueItem()).thenReturn(true);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(trackUrn)));

        controller.togglePlayback();

        verify(playbackStrategy).togglePlayback();
    }

    @Test
    public void togglePlaybackShouldTogglePlaybackOnPlaybackStrategyIfPlayingCurrentTrack() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueItem()).thenReturn(true);
        controller.togglePlayback();

        verify(playbackStrategy).togglePlayback();
    }

    @Test
    public void togglePlaybackShouldPlayCurrentOnPlaybackStrategyIfPlayingCurrentTrackAndInErrorState() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueItem()).thenReturn(true);
        when(playSessionStateProvider.isInErrorState()).thenReturn(true);
        controller.togglePlayback();

        assertThat(playCurrentSubject.hasObservers()).isTrue();
        verify(playbackStrategy, never()).togglePlayback();
    }

    @Test
    public void togglePlaybackShouldNotTogglePlaybackOnPlaybackStrategyIfNotPlayingCurrentTrack() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueItem()).thenReturn(false);
        controller.togglePlayback();

        verify(playbackStrategy, never()).togglePlayback();
    }

    @Test
    public void playCurrentCallsPlayCurrentOnPlaybackStrategy() {
        controller.playCurrent();

        assertThat(playCurrentSubject.hasObservers()).isTrue();
    }

    @Test
    public void playCurrentWhenEmptyCallsLoadsQueueBeforePlayingCurrentOnPlaybackStrategy() {
        final PublishSubject<PlayQueue> subject = PublishSubject.create();
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        when(playQueueManager.loadPlayQueueAsync()).thenReturn(subject);

        controller.playCurrent();

        assertThat(playCurrentSubject.hasObservers()).isFalse();

        subject.onNext(PlayQueue.empty());

        assertThat(playCurrentSubject.hasObservers()).isTrue();
    }

    @Test
    public void settingPlayQueueItemPublishesAdSkippedTrackingEventWhenTrackIsAudioAd() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS + 1);
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);

        controller.setCurrentPlayQueueItem(trackPlayQueueItem);

        // make sure we test for the current track being an ad *before* we skip
        InOrder inOrder = inOrder(adsOperations, playQueueManager);
        inOrder.verify(adsOperations, atLeastOnce()).isCurrentItemAudioAd();
        inOrder.verify(playQueueManager).setCurrentPlayQueueItem(trackPlayQueueItem);

        final UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(UIEvent.KIND_SKIP_AUDIO_AD_CLICK);
        assertThat(event.getAttributes().get("ad_track_urn")).isEqualTo(Urn.forTrack(123).toString());
    }

    @Test
    public void settingPlayQueueItemAttemptsAdDeliveryEventPublishIfTrackChanged() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(321L)));
        controller.setCurrentPlayQueueItem(trackPlayQueueItem);
        verify(adsController).publishAdDeliveryEventIfUpcoming();
    }

    @Test
    public void settingPlayQueueItemDoesNotPublishAdSkippedTrackingEventWhenTrackNotAnAd() {
        controller.setCurrentPlayQueueItem(trackPlayQueueItem);

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void previousTrackCallsMoveToPreviousTrackOnPlayQueueManagerIfProgressLessThanTolerance() {
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(2999L, 5000));

        controller.previousTrack();

        verify(playQueueManager).moveToPreviousPlayableItem();
    }

    @Test
    public void previousTrackCallsMoveToPreviousTrackOnPlayQueueManagerIfProgressEqualToleranceAndPlayingAudioAd() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(3000L, 5000));

        controller.previousTrack();

        verify(playQueueManager).moveToPreviousPlayableItem();
    }

    @Test
    public void previousTrackSeeksToZeroIfProgressEqualToTolerance() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueItem()).thenReturn(true);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(3000L, 5000));

        controller.previousTrack();

        verify(playbackStrategy).seek(0);
    }

    @Test
    public void previousTrackSeeksToZeroIfProgressGreaterThanTolerance() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueItem()).thenReturn(true);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(3001L, 5000));

        controller.previousTrack();

        verify(playbackStrategy).seek(0);
    }

    @Test
    public void previousTrackCallsPreviousItemIfPlayingAudioAdWithProgressEqualToTimeout() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(PlaybackProgress.empty());

        controller.previousTrack();

        verify(playQueueManager).moveToPreviousPlayableItem();
    }

    @Test
    public void previousTrackDoesNothingIfPlayingAudioAdWithProgressLessThanTimeout() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        controller.previousTrack();

        verify(playQueueManager, never()).moveToPreviousPlayableItem();
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
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);

        controller.previousTrack();

        // make sure we test for the current track being an ad *before* we skip
        InOrder inOrder = inOrder(adsOperations, playQueueManager);
        inOrder.verify(adsOperations, atLeastOnce()).isCurrentItemAd();
        inOrder.verify(playQueueManager).moveToPreviousPlayableItem();

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

        verify(playQueueManager).moveToNextPlayableItem();
    }

    @Test
    public void nextTrackCallsNextItemIfPlayingAudioAdWithProgressEqualToTimeout() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS);

        controller.nextTrack();

        verify(playQueueManager).moveToNextPlayableItem();
    }

    @Test
    public void nextTrackDoesNothingIfPlayingAudioAdWithProgressLessThanTimeout() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        controller.nextTrack();

        verify(playQueueManager, never()).moveToNextPlayableItem();
    }

    @Test
    public void nextTrackPublishesAdSkippedTrackingEventWhenTrackIsAudioAd() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS + 1);
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);

        controller.nextTrack();

        // make sure we test for the current track being an ad *before* we skip
        InOrder inOrder = inOrder(adsOperations, playQueueManager);
        inOrder.verify(adsOperations, atLeastOnce()).isCurrentItemAd();
        inOrder.verify(playQueueManager).moveToNextPlayableItem();

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
    public void seeksToProvidedPositionIfServiceIsPlayingCurrentItem() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueItem()).thenReturn(true);
        controller.seek(350L);

        verify(playbackStrategy).seek(350L);
    }

    @Test
    public void seeksSavesPlayQueueProgressToSeekPositionIfNotPlayingCurrentItem() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueItem()).thenReturn(false);
        controller.seek(350L);

        verify(playQueueManager).saveCurrentProgress(350L);
    }

    @Test
    public void seekSeeksToProvidedPositionIfPlayingAudioAdWithProgressEqualTimeout() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueItem()).thenReturn(true);
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
        final PlayQueue playQueue = TestPlayQueue.fromUrns(Arrays.asList(track), playSessionSource);
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
        controller.playNewQueue(TestPlayQueue.fromUrns(Arrays.asList(track), PlaySessionSource.EMPTY), track, 0, PlaySessionSource.EMPTY)
                .subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).isSuccess()).isFalse();
        assertThat(subscriber.getOnNextEvents().get(0).getErrorReason()).isEqualTo(PlaybackResult.ErrorReason.UNSKIPPABLE);
    }

    @Test
    public void playNewQueueDoesNotPlayCurrentTrackIfErrorSettingQueue() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);
        Urn track = Urn.forTrack(123L);

        final TestSubscriber<PlaybackResult> subscriber = new TestSubscriber<>();
        controller.playNewQueue(TestPlayQueue.fromUrns(Arrays.asList(track), PlaySessionSource.EMPTY), track, 0, PlaySessionSource.EMPTY)
                .subscribe(subscriber);

        assertThat(playCurrentSubject.hasObservers()).isFalse();
    }

    @Test
    public void playNewQueueUnsubscribesFromCurrentTrackLoad() {
        Urn track = Urn.forTrack(123L);

        final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.ACTIVITIES);
        final PlayQueue playQueue = TestPlayQueue.fromUrns(Arrays.asList(track), playSessionSource);
        setupSetNewQueue(track, playSessionSource, playQueue, Observable.<PlaybackResult>never());

        controller.playCurrent();

        assertThat(playCurrentSubject.hasObservers()).isTrue();

        controller.playNewQueue(playQueue, track, 0, playSessionSource).subscribe(new TestSubscriber<PlaybackResult>());

        assertThat(playCurrentSubject.hasObservers()).isFalse();
    }

    @Test
    public void playCurrentUnsubscribesFromPreviousTrackLoad() {
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
        final PlayQueue playQueue = TestPlayQueue.fromUrns(Arrays.asList(track), playSessionSource);
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

    @Test
    public void resetsPlaySession() {
        controller.resetPlaySession();

        verify(playbackServiceInitiator).resetPlaybackService();
        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_UI, PlayerUIEvent.class).getKind())
                .isEqualTo(PlayerUIEvent.fromPlayerCollapsed().getKind());
    }

    private void setupSetNewQueue(Urn track, PlaySessionSource playSessionSource, PlayQueue playQueue, Observable<PlaybackResult> result) {
        when(playbackStrategy.setNewQueue(playQueue, track, 0, playSessionSource))
                .thenReturn(result);
    }

    private void setupAdInProgress(long currentProgress) {
        final PlaybackProgress progress = new PlaybackProgress(currentProgress, 30000);
        final AudioAd adData = AdFixtures.getAudioAd(Urn.forTrack(456L));
        final PlayQueueItem playQueueItem = TestPlayQueueItem.createTrack(Urn.forTrack(123L), adData);

        when(playSessionStateProvider.getLastProgressEventForCurrentPlayQueueItem()).thenReturn(progress);
        when(adsOperations.isCurrentItemAd()).thenReturn(true);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(playQueueItem);
    }
}
