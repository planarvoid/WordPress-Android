package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueue;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.Collections;

public class PlaySessionControllerTest extends AndroidUnitTest {

    private PlayQueueItem trackPlayQueueItem;
    private Urn trackUrn;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private AdsOperations adsOperations;
    @Mock private AdsController adsController;
    @Mock private CastConnectionHelper castConnectionHelper;
    @Mock private NetworkConnectionHelper networkConnectionHelper;
    @Mock private PlaybackStrategy playbackStrategy;
    @Mock private PlaybackToastHelper playbackToastHelper;
    @Mock private AccountOperations accountOperations;
    @Mock private FeatureFlags featureFlags;
    @Mock private PlaybackServiceController playbackServiceController;

    private PlaySessionController controller;
    private PublishSubject<Void> playCurrentSubject;

    @Before
    public void setUp() throws Exception {
        controller = new PlaySessionController(eventBus,
                                               adsOperations,
                                               adsController,
                                               playQueueManager,
                                               playSessionStateProvider,
                                               castConnectionHelper,
                                               InjectionSupport.providerOf(playbackStrategy),
                                               playbackToastHelper,
                                               playbackServiceController);
        controller.subscribe();

        trackUrn = Urn.forTrack(123);
        trackPlayQueueItem = TestPlayQueueItem.createTrack(trackUrn);
        playCurrentSubject = PublishSubject.create();

        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(playQueueManager.isCurrentTrack(trackUrn)).thenReturn(true);
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo("origin screen", true));
        when(playQueueManager.getCollectionUrn()).thenReturn(Urn.NOT_SET);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(PlaySessionSource.EMPTY);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(456L));
        when(playbackStrategy.playCurrent()).thenReturn(playCurrentSubject);
        when(playQueueManager.getUpcomingPlayQueueItems(anyInt())).thenReturn(Lists.<Urn>newArrayList());
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(PlaybackProgress.empty());
    }

    @Test
    public void playQueueTrackChangedHandlerCallsPlayCurrentIfCurrentItemIsVideoAdIfThePlayerIsInPlaySession() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        final VideoAdQueueItem videoItem = TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(trackUrn));

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(videoItem, Urn.NOT_SET, 0));

        assertThat(playCurrentSubject.hasObservers()).isTrue();
    }

    @Test
    public void playQueueTrackChangedHandlerCallsPlayCurrentIfThePlayerIsInPlaySession() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));

        assertThat(playCurrentSubject.hasObservers()).isTrue();
    }

    @Test
    public void playQueueTrackChangedHandlerCallsPlayCurrentAndPausesIfNextTrackBlocked() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        when(playbackStrategy.playCurrent()).thenReturn(Observable.<Void>error(new BlockedTrackException(trackUrn)));

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playbackStrategy).pause();
    }

    @Test
    public void playQueueTrackChangedHandlerDoesNotCallPlayCurrentForTrackIfPlaySessionIsNotActive() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.idle());
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        assertThat(playCurrentSubject.hasObservers()).isFalse();
    }

    @Test
    public void playQueueTrackChangeHandlerDoesNotCallPlayCurrentForVideoAdIfPlaySessionIsNotActive() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.idle());

        final VideoAdQueueItem videoItem = TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(trackUrn));
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromNewQueue(videoItem, Urn.NOT_SET, 0));

        assertThat(playCurrentSubject.hasObservers()).isFalse();
    }

    @Test
    public void playQueueTrackChangeWhenCastingPlaysTrackWhenCurrentTrackIsDifferentAndPlaying() {
        when(castConnectionHelper.isCasting()).thenReturn(true);
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        when(playbackStrategy.playCurrent()).thenReturn(playCurrentSubject);

        final PlayQueueItem newPlayQueueItem = TestPlayQueueItem.createTrack(Urn.forTrack(2));
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(newPlayQueueItem, Urn.NOT_SET, 0));

        assertThat(playCurrentSubject.hasObservers()).isTrue();
    }

    @Test
    public void shouldNotRespondToPlayQueueTrackChangesWhenPlayerIsNotPlaying() {
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        assertThat(playCurrentSubject.hasObservers()).isFalse();
    }

    @Test
    public void playQueueTrackChangeHandlerShouldPlayCurrentOnTrackIfPreviousItemEndedInError() {
        when(playSessionStateProvider.isInErrorState()).thenReturn(true);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        assertThat(playCurrentSubject.hasObservers()).isTrue();
    }

    @Test
    public void playQueueTrackChangedHandlerClearsLastProgress() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        when(playbackStrategy.playCurrent()).thenReturn(playCurrentSubject);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playSessionStateProvider).clearLastProgressForItem(trackUrn);
    }

    @Test
    public void playQueueTrackChangedIsIgnoredWhenAlreadyPlayingTrack() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        when(playbackStrategy.playCurrent()).thenReturn(playCurrentSubject);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));

        assertThat(playCurrentSubject.hasObservers()).isTrue();
        playCurrentSubject.onCompleted();

        final PublishSubject<Void> nextPlayCurrentSubject = PublishSubject.create();
        when(playbackStrategy.playCurrent()).thenReturn(nextPlayCurrentSubject);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));
        assertThat(nextPlayCurrentSubject.hasObservers()).isFalse();
    }

    @Test
    public void playQueueTrackPlaysWhenLastTrackIsSameAndEventIsFromPositionRepeat() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        when(playbackStrategy.playCurrent()).thenReturn(playCurrentSubject);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));

        assertThat(playCurrentSubject.hasObservers()).isTrue();
        playCurrentSubject.onCompleted();

        final PublishSubject<Void> nextPlayCurrentSubject = PublishSubject.create();
        when(playbackStrategy.playCurrent()).thenReturn(nextPlayCurrentSubject);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionRepeat(trackPlayQueueItem, Urn.NOT_SET, 0));
        assertThat(nextPlayCurrentSubject.hasObservers()).isTrue();
    }

    @Test
    public void onStateTransitionForQueueCompleteDoesNotSavePosition() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.playQueueComplete());
        verify(playQueueManager, never()).saveCurrentPosition();
    }

    @Test
    public void onStateTransitionForBufferingDoesNotSaveQueuePosition() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.buffering());
        verify(playQueueManager, never()).saveCurrentPosition();
    }

    @Test
    public void onStateTransitionForPlayingDoesNotSaveQueuePosition() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.playing());
        verify(playQueueManager, never()).saveCurrentPosition();
    }

    @Test
    public void togglePlaybackShouldTogglePlaybackStrategyIfVideoAd() {
        final VideoAdQueueItem queueItem = TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(trackUrn));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(queueItem);
        when(playSessionStateProvider.isCurrentlyPlaying(queueItem.getUrn())).thenReturn(true);

        controller.togglePlayback();

        verify(playbackStrategy).togglePlayback();
    }

    @Test
    public void togglePlaybackShouldTogglePlaybackOnPlaybackStrategyIfPlayingCurrentTrack() {
        when(playSessionStateProvider.isCurrentlyPlaying(trackUrn)).thenReturn(true);
        controller.togglePlayback();

        verify(playbackStrategy).togglePlayback();
    }

    @Test
    public void togglePlaybackShouldPlayCurrentOnPlaybackStrategyIfPlayingCurrentTrackAndInErrorState() {
        when(playSessionStateProvider.isCurrentlyPlaying(trackUrn)).thenReturn(true);
        when(playSessionStateProvider.isInErrorState()).thenReturn(true);
        controller.togglePlayback();

        assertThat(playCurrentSubject.hasObservers()).isTrue();
        verify(playbackStrategy, never()).togglePlayback();
    }

    @Test
    public void togglePlaybackShouldNotTogglePlaybackOnPlaybackStrategyIfNotPlayingCurrentTrack() {
        when(playSessionStateProvider.isCurrentlyPlaying(trackUrn)).thenReturn(false);
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
        setupAudioAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS + 1);
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);

        controller.setCurrentPlayQueueItem(trackPlayQueueItem);

        // make sure we test for the current track being an ad *before* we skip
        InOrder inOrder = inOrder(adsOperations, playQueueManager);
        inOrder.verify(adsOperations, atLeastOnce()).isCurrentItemAd();
        inOrder.verify(playQueueManager).setCurrentPlayQueueItem(trackPlayQueueItem);

        final UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.kind()).isEqualTo(UIEvent.Kind.SKIP_AD_CLICK);
        assertThat(event.adUrn().get()).isEqualTo(Urn.forAd("dfp", "869").toString());
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
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(2999L, 5000, trackUrn));

        controller.previousTrack();

        verify(playQueueManager).moveToPreviousPlayableItem();
    }

    @Test
    public void previousTrackCallsMoveToPreviousTrackOnPlayQueueManagerIfProgressGreaterThanToleranceAndPlayingAudioAd() {
        setupAudioAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS);

        controller.previousTrack();

        verify(playQueueManager).moveToPreviousPlayableItem();
    }

    @Test
    public void previousTrackSeeksToZeroIfProgressEqualToTolerance() {
        when(playSessionStateProvider.isCurrentlyPlaying(trackUrn)).thenReturn(true);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(3000L, 5000, trackUrn));

        controller.previousTrack();

        verify(playbackStrategy).seek(0);
    }

    @Test
    public void previousTrackSeeksToZeroIfProgressGreaterThanTolerance() {
        when(playSessionStateProvider.isCurrentlyPlaying(trackUrn)).thenReturn(true);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(3001L, 5000, trackUrn));

        controller.previousTrack();

        verify(playbackStrategy).seek(0);
    }

    @Test
    public void previousTrackCallsPreviousItemIfPlayingAdWithProgressEqualToTimeout() {
        setupAudioAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS);

        controller.previousTrack();

        verify(playQueueManager).moveToPreviousPlayableItem();
    }

    @Test
    public void previousTrackDoesNothingIfPlayingAdWithProgressLessThanTimeout() {
        setupAudioAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        controller.previousTrack();

        verify(playQueueManager, never()).moveToPreviousPlayableItem();
    }

    @Test
    public void previousTrackShowsUnskippableToastWhenPlaybackNotSkippable() {
        setupAudioAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        controller.previousTrack();

        verify(playbackToastHelper).showUnskippableAdToast();
    }

    @Test
    public void previousTrackPublishesAdSkippedTrackingEventWhenTrackIsAudioAd() {
        setupAudioAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS + 1);

        controller.previousTrack();

        // make sure we test for the current track being an ad *before* we skip
        InOrder inOrder = inOrder(adsOperations, playQueueManager);
        inOrder.verify(adsOperations, atLeastOnce()).isCurrentItemAd();
        inOrder.verify(playQueueManager).moveToPreviousPlayableItem();

        final UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.kind()).isEqualTo(UIEvent.Kind.SKIP_AD_CLICK);
        assertThat(event.adUrn().get()).isEqualTo(Urn.forAd("dfp", "869").toString());
    }

    @Test
    public void previousTrackPublishesAdSkippedTrackingEventWhenTrackIsVideoAd() {
        when(playSessionStateProvider.isCurrentlyPlaying(trackUrn)).thenReturn(true);
        setupVideoAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS + 1);

        controller.previousTrack();

        // make sure we test for the current track being an ad *before* we skip
        InOrder inOrder = inOrder(adsOperations, playQueueManager);
        inOrder.verify(adsOperations, atLeastOnce()).isCurrentItemAd();
        inOrder.verify(playQueueManager).moveToPreviousPlayableItem();

        final UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.kind()).isEqualTo(UIEvent.Kind.SKIP_AD_CLICK);
    }

    @Test
    public void previousTrackDoesNotPublishAdSkippedTrackingEventWhenAdNotYetSkippable() {
        setupAudioAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        controller.previousTrack();

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void previousTrackDoesNotPublishAdSkippedTrackingEventWhenTrackNotAnAd() {
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(3000L, 5000, trackUrn));

        controller.previousTrack();

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void nextTrackShowsUnskippableToastWhenPlaybackNotSkippable() {
        setupAudioAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        controller.nextTrack();

        verify(playbackToastHelper).showUnskippableAdToast();
    }

    @Test
    public void nextTrackCallsNextItemOnPlayQueueManager() {
        controller.nextTrack();

        verify(playQueueManager).moveToNextPlayableItem();
    }

    @Test
    public void nextTrackCallsNextItemIfPlayingAdWithProgressEqualToTimeout() {
        setupAudioAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS);

        controller.nextTrack();

        verify(playQueueManager).moveToNextPlayableItem();
    }

    @Test
    public void nextTrackDoesNothingIfPlayingAdWithProgressLessThanTimeout() {
        setupAudioAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        controller.nextTrack();

        verify(playQueueManager, never()).moveToNextPlayableItem();
    }

    @Test
    public void nextTrackPublishesAdSkippedTrackingEventWhenTrackIsAudioAd() {
        setupAudioAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS + 1);

        controller.nextTrack();

        // make sure we test for the current track being an ad *before* we skip
        InOrder inOrder = inOrder(adsOperations, playQueueManager);
        inOrder.verify(adsOperations, atLeastOnce()).isCurrentItemAd();
        inOrder.verify(playQueueManager).moveToNextPlayableItem();

        final UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.kind()).isEqualTo(UIEvent.Kind.SKIP_AD_CLICK);
        assertThat(event.adUrn().get()).isEqualTo(Urn.forAd("dfp", "869").toString());
    }

    @Test
    public void nextTrackPublishesAdSkippedTrackingEventWhenTrackIsVideoAd() {
        setupVideoAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS + 1);

        controller.nextTrack();

        // make sure we test for the current track being an ad *before* we skip
        InOrder inOrder = inOrder(adsOperations, playQueueManager);
        inOrder.verify(adsOperations, atLeastOnce()).isCurrentItemAd();
        inOrder.verify(playQueueManager).moveToNextPlayableItem();

        final UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.kind()).isEqualTo(UIEvent.Kind.SKIP_AD_CLICK);
    }

    @Test
    public void nextTrackDoesNotPublishAdSkippedTrackingEventWhenAdNotYetSkippable() {
        setupAudioAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

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
        when(playSessionStateProvider.isCurrentlyPlaying(trackUrn)).thenReturn(true);
        controller.seek(350L);

        verify(playbackStrategy).seek(350L);
    }

    @Test
    public void seeksSavesPlayQueueProgressToSeekPositionIfNotPlayingCurrentItem() {
        when(playSessionStateProvider.isCurrentlyPlaying(trackUrn)).thenReturn(false);
        controller.seek(350L);

        verify(playQueueManager).saveCurrentPosition();
    }

    @Test
    public void seekSeeksToProvidedPositionIfPlayingAudioAdWithProgressEqualTimeout() {
        when(playSessionStateProvider.isCurrentlyPlaying(trackUrn)).thenReturn(true);
        setupAudioAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS);

        controller.seek(350L);

        verify(playbackStrategy).seek(350L);
    }

    @Test
    public void seekDoesNothingIfPlayingAudioAdWithProgressLessThanTimeout() {
        setupAudioAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        controller.seek(350L);

        verifyZeroInteractions(playbackStrategy);
    }

    @Test
    public void playNewQueueWhenSkippablePlaysQueue() {
        Urn track = Urn.forTrack(123L);

        final TestSubscriber<PlaybackResult> subscriber = new TestSubscriber<>();
        final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.ACTIVITIES);
        final PlayQueue playQueue = TestPlayQueue.fromUrns(Collections.singletonList(track), playSessionSource);
        setupSetNewQueue(track, playSessionSource, playQueue, Observable.just(PlaybackResult.success()));

        controller.playNewQueue(playQueue, track, 0, playSessionSource)
                  .subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).isSuccess()).isTrue();
    }

    @Test
    public void playNewQueueWhenUnskippableReturnsPlaybackError() {
        setupAudioAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);
        Urn track = Urn.forTrack(123L);

        final TestSubscriber<PlaybackResult> subscriber = new TestSubscriber<>();
        controller.playNewQueue(TestPlayQueue.fromUrns(Collections.singletonList(track), PlaySessionSource.EMPTY),
                                track,
                                0,
                                PlaySessionSource.EMPTY)
                  .subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).isSuccess()).isFalse();
        assertThat(subscriber.getOnNextEvents()
                             .get(0)
                             .getErrorReason()).isEqualTo(PlaybackResult.ErrorReason.UNSKIPPABLE);
    }

    @Test
    public void playNewQueueDoesNotPlayCurrentTrackIfErrorSettingQueue() {
        setupAudioAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);
        Urn track = Urn.forTrack(123L);

        final TestSubscriber<PlaybackResult> subscriber = new TestSubscriber<>();
        controller.playNewQueue(TestPlayQueue.fromUrns(Collections.singletonList(track), PlaySessionSource.EMPTY),
                                track,
                                0,
                                PlaySessionSource.EMPTY)
                  .subscribe(subscriber);

        assertThat(playCurrentSubject.hasObservers()).isFalse();
    }

    @Test
    public void playNewQueueUnsubscribesFromCurrentTrackLoad() {
        Urn track = Urn.forTrack(123L);

        final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.ACTIVITIES);
        final PlayQueue playQueue = TestPlayQueue.fromUrns(Collections.singletonList(track), playSessionSource);
        setupSetNewQueue(track, playSessionSource, playQueue, Observable.<PlaybackResult>never());

        controller.playCurrent();

        assertThat(playCurrentSubject.hasObservers()).isTrue();

        controller.playNewQueue(playQueue, track, 0, playSessionSource).subscribe(new TestSubscriber<PlaybackResult>());

        assertThat(playCurrentSubject.hasObservers()).isFalse();
    }

    @Test
    public void playCurrentUnsubscribesFromPreviousTrackLoad() {
        when(playbackStrategy.playCurrent())
                .thenReturn(playCurrentSubject)
                .thenReturn(Observable.<Void>never());

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
        final PlayQueue playQueue = TestPlayQueue.fromUrns(Collections.singletonList(track), playSessionSource);
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

        verify(playbackServiceController).resetPlaybackService();
        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_UI, PlayerUIEvent.class).getKind())
                .isEqualTo(PlayerUIEvent.fromPlayerCollapsed().getKind());
    }

    @Test
    public void playResumesWhenIsPlayingCurrent() {
        when(playSessionStateProvider.isCurrentlyPlaying(trackUrn)).thenReturn(true);

        controller.play();

        verify(playbackStrategy).resume();
        assertThat(playCurrentSubject.hasObservers()).isFalse();
    }

    @Test
    public void playPlaysCurrentWhenIsNotPlayingCurrent() {
        when(playSessionStateProvider.isCurrentlyPlaying(trackUrn)).thenReturn(false);

        controller.play();

        verify(playbackStrategy, never()).resume();
        assertThat(playCurrentSubject.hasObservers()).isTrue();
    }

    private void setupSetNewQueue(Urn track,
                                  PlaySessionSource playSessionSource,
                                  PlayQueue playQueue,
                                  Observable<PlaybackResult> result) {
        when(playbackStrategy.setNewQueue(playQueue, track, 0, playSessionSource))
                .thenReturn(result);
    }

    private void setupVideoAdInProgress(long currentProgress) {
        final VideoAd adData = AdFixtures.getVideoAd(Urn.forTrack(456L));
        final VideoAdQueueItem videoItem = TestPlayQueueItem.createVideo(adData);
        when(adsOperations.isCurrentItemVideoAd()).thenReturn(true);
        setupAdInProgress(currentProgress, videoItem);
    }

    private void setupAudioAdInProgress(long currentProgress) {
        final AudioAd adData = AdFixtures.getAudioAd(Urn.forTrack(456L));
        final PlayQueueItem audioAdItem = TestPlayQueueItem.createAudioAd(adData);
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);
        setupAdInProgress(currentProgress, audioAdItem);
    }

    private void setupAdInProgress(long currentProgress, PlayQueueItem playQueueItem) {
        final PlaybackProgress progress = new PlaybackProgress(currentProgress, 30000, trackUrn);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(playQueueItem);
        when(playSessionStateProvider.isCurrentlyPlaying(playQueueItem.getUrn())).thenReturn(true);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(progress);
        when(adsOperations.isCurrentItemAd()).thenReturn(true);
        when(adsOperations.getCurrentTrackAdData()).thenReturn(playQueueItem.getAdData());
    }
}
