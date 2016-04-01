package com.soundcloud.android.ads;

import com.soundcloud.android.playback.PlaybackState;
import com.soundcloud.android.playback.PlayStateReason;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.AdDeliveryEvent;
import com.soundcloud.android.events.AdTrackingKeys;
import com.soundcloud.android.events.AdFailedToBufferEvent;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.playback.VideoQueueItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestEvents;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

import android.support.v7.app.AppCompatActivity;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class AdsControllerTest extends AndroidUnitTest {

    private final Urn currentTrackUrn = Urn.forTrack(122L);
    private final PlayQueueItem currentPlayQueueItem = TestPlayQueueItem.createTrack(currentTrackUrn);
    private final Urn nextTrackUrn = Urn.forTrack(456L);
    private final TrackQueueItem nextPlayQueueItem = TestPlayQueueItem.createTrack(nextTrackUrn);

    private final PropertySet currentMonetizablePropertySet = PropertySet.from(TrackProperty.URN.bind(currentTrackUrn), TrackProperty.MONETIZABLE.bind(true));
    private final PropertySet nextMonetizablePropertySet = PropertySet.from(TrackProperty.URN.bind(nextTrackUrn), TrackProperty.MONETIZABLE.bind(true));
    private final PropertySet nextNonMonetizablePropertySet = PropertySet.from(TrackProperty.URN.bind(nextTrackUrn), TrackProperty.MONETIZABLE.bind(false));

    @Mock private PlayQueueManager playQueueManager;
    @Mock private AdsOperations adsOperations;
    @Mock private TrackRepository trackRepository;
    @Mock private AccountOperations accountOperations;
    @Mock private VisualAdImpressionOperations visualAdImpressionOperations;
    @Mock private AdOverlayImpressionOperations adOverlayImpressionOperations;

    private TestEventBus eventBus = new TestEventBus();
    private TestScheduler scheduler = Schedulers.test();
    private ApiAdsForTrack apiAdsForTrack;
    private AdsController adsController;

    @Before
    public void setUp() throws Exception {
        when(visualAdImpressionOperations.trackImpression()).thenReturn(Observable.<TrackingEvent>never());
        when(adOverlayImpressionOperations.trackImpression()).thenReturn(Observable.<TrackingEvent>never());
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(nextNonMonetizablePropertySet));

        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(currentPlayQueueItem);
        when(playQueueManager.getNextPlayQueueItem()).thenReturn(nextPlayQueueItem);
        when(playQueueManager.isCurrentItem(currentPlayQueueItem)).thenReturn(true);
        when(playQueueManager.isNextItem(nextPlayQueueItem)).thenReturn(true);

        adsController = new AdsController(eventBus, adsOperations, visualAdImpressionOperations, adOverlayImpressionOperations,
                playQueueManager, trackRepository, scheduler);
        apiAdsForTrack = AdFixtures.fullAdsForTrack();
    }

    @Test
    public void trackChangeEventInsertsAdForNextTrack() throws CreateModelException {
        insertFullAdsForNextTrack();

        verify(adsOperations).applyAdToUpcomingTrack(apiAdsForTrack);
    }

    @Test
    public void trackChangeEventInsertsInterstitialForCurrentTrackIntoPlayQueue() throws CreateModelException {
        when(trackRepository.track(currentTrackUrn)).thenReturn(Observable.just(currentMonetizablePropertySet));
        when(adsOperations.ads(eq(currentTrackUrn), anyBoolean(), anyBoolean())).thenReturn(Observable.just(apiAdsForTrack));
        adsController.subscribe();
        final TrackQueueItem trackItem = TestPlayQueueItem.createTrack(currentTrackUrn, InterstitialAd.create(apiAdsForTrack.interstitialAd().get(), currentTrackUrn));
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(trackItem, Urn.NOT_SET, 0));

        verify(adsOperations).applyInterstitialToTrack(currentPlayQueueItem, apiAdsForTrack);
    }

    @Test
    public void trackChangeEventDoesNotFetchAdForPlaylistQueueItem() throws CreateModelException {
        when(playQueueManager.hasNextItem()).thenReturn(true);
        when(playQueueManager.getNextPlayQueueItem()).thenReturn(TestPlayQueueItem.createPlaylist(Urn.forTrack(123)));
        when(trackRepository.track(nextTrackUrn)).thenReturn(Observable.<PropertySet>empty());
        adsController.subscribe();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(currentPlayQueueItem, Urn.NOT_SET, 0));
        verify(adsOperations, never()).ads(any(Urn.class), anyBoolean(), anyBoolean());
    }

    @Test
    public void trackChangeEventDoesNotFetchTrackFromStorageIfAlreadyTryingToFetchAd() throws CreateModelException {
        when(playQueueManager.hasTrackAsNextItem()).thenReturn(true);
        when(trackRepository.track(nextTrackUrn)).thenReturn(Observable.<PropertySet>empty());
        adsController.subscribe();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(currentPlayQueueItem, Urn.NOT_SET, 0));
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate(Urn.NOT_SET));
        verify(trackRepository).track(nextTrackUrn);
    }


    @Test
    public void playQueueUpdateEventInsertsAudioAdIntoPlayQueue() throws CreateModelException {
        when(playQueueManager.hasTrackAsNextItem()).thenReturn(true);
        when(trackRepository.track(nextTrackUrn)).thenReturn(Observable.just(nextMonetizablePropertySet));
        when(adsOperations.ads(eq(nextTrackUrn), anyBoolean(), anyBoolean())).thenReturn(Observable.just(apiAdsForTrack));
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate(Urn.NOT_SET));

        verify(adsOperations).applyAdToUpcomingTrack(apiAdsForTrack);
    }

    @Test
    public void newQueueEventDoesNotInsertAudioAdIntoPlayQueue() throws CreateModelException {
        when(playQueueManager.hasNextItem()).thenReturn(true);
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(nextMonetizablePropertySet));
        when(adsOperations.ads(any(Urn.class), anyBoolean(), anyBoolean())).thenReturn(Observable.just(apiAdsForTrack));
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(Urn.NOT_SET));

        verify(adsOperations, never()).applyAdToUpcomingTrack(any(ApiAdsForTrack.class));
    }

    @Test
    public void configureAdForNextTrackReplacesVideoAdWhenAppInBackground() {
        final VideoQueueItem videoItem = TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(123L)));
        insertFullAdsForNextTrack();

        when(adsOperations.isNextItemAd()).thenReturn(true);
        when(playQueueManager.hasNextItem()).thenReturn(true);
        when(playQueueManager.getNextPlayQueueItem()).thenReturn(videoItem, nextPlayQueueItem);

        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnPause(mock(AppCompatActivity.class)));
        adsController.reconfigureAdForNextTrack();

        verify(adsOperations).replaceUpcomingVideoAd(any(ApiAdsForTrack.class), any(VideoQueueItem.class));
    }

    @Test
    public void configureAdForNextTrackInsertsAudioAdWhenAppInBackgroundAndTheresNoExistingAdInPlayQueue() {
        insertFullAdsForNextTrack();

        when(adsOperations.isNextItemAd()).thenReturn(false);
        when(playQueueManager.hasNextItem()).thenReturn(true);
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnPause(mock(AppCompatActivity.class)));

        adsController.reconfigureAdForNextTrack();

        verify(adsOperations).insertAudioAd(nextPlayQueueItem, apiAdsForTrack.audioAd().get());
    }

    @Test
    public void configureAdForNextTrackDoesntInsertAudioAdWhenAppInForeground() {
        insertFullAdsForNextTrack();

        when(adsOperations.isNextItemAd()).thenReturn(false);
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnResume(mock(AppCompatActivity.class)));

        adsController.reconfigureAdForNextTrack();

        verify(adsOperations, never()).insertAudioAd(any(TrackQueueItem.class), any(ApiAudioAd.class));
    }

    @Test
    public void configureAdForNextTrackInsertsAudioAdWhenNoActivityLifecycleEventsObserved() {
        insertFullAdsForNextTrack();

        when(adsOperations.isNextItemAd()).thenReturn(false);
        when(playQueueManager.hasNextItem()).thenReturn(true);

        adsController.reconfigureAdForNextTrack();

        verify(adsOperations).insertAudioAd(any(TrackQueueItem.class), any(ApiAudioAd.class));
    }

    @Test
    public void configureAdForNextTrackDoesNotReplaceAnExistingAudioAd() {
        final PlayQueueItem audioAd = TestPlayQueueItem.createTrack(Urn.forTrack(123L), AdFixtures.getAudioAd(Urn.forTrack(123L)));
        insertFullAdsForNextTrack();

        when(playQueueManager.getNextPlayQueueItem()).thenReturn(audioAd);
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnPause(mock(AppCompatActivity.class)));

        adsController.reconfigureAdForNextTrack();

        verify(adsOperations, never()).insertAudioAd(any(TrackQueueItem.class), any(ApiAudioAd.class));
    }

    @Test
    public void configureAdForNextTrackDoesNothingWithNoAdsForNextTrack() {
        when(adsOperations.isNextItemAd()).thenReturn(false);
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnPause(mock(AppCompatActivity.class)));

        adsController.reconfigureAdForNextTrack();

        verify(adsOperations, never()).insertAudioAd(any(TrackQueueItem.class), any(ApiAudioAd.class));
    }

    @Test
    public void trackChangeEventDoesNothingIfNoNextTrack() {
        when(playQueueManager.hasNextItem()).thenReturn(false);
        adsController.subscribe();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(currentTrackUrn), Urn.NOT_SET, 0));

        verify(adsOperations, never()).applyAdToUpcomingTrack(any(ApiAdsForTrack.class));
    }

    @Test
    public void trackChangeEventDoesNothingIfNextTrackIsAudioAd() {
        when(trackRepository.track(nextTrackUrn)).thenReturn(Observable.just(nextMonetizablePropertySet));
        when(adsOperations.ads(eq(nextTrackUrn), anyBoolean(), anyBoolean())).thenReturn(Observable.just(apiAdsForTrack));
        when(playQueueManager.hasNextItem()).thenReturn(true);
        when(adsOperations.isNextItemAd()).thenReturn(true);
        adsController.subscribe();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(currentTrackUrn), Urn.NOT_SET, 0));

        verify(adsOperations, never()).applyAdToUpcomingTrack(any(ApiAdsForTrack.class));
    }

    @Test
    public void trackChangeEventDoesNotApplyAdIfAlreadyPlayingAd() {
        when(trackRepository.track(nextTrackUrn)).thenReturn(Observable.just(nextMonetizablePropertySet));
        when(adsOperations.ads(eq(nextTrackUrn), anyBoolean(), anyBoolean())).thenReturn(Observable.just(apiAdsForTrack));
        when(playQueueManager.hasNextItem()).thenReturn(true);
        when(adsOperations.isCurrentItemAd()).thenReturn(true);
        adsController.subscribe();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(currentTrackUrn), Urn.NOT_SET, 0));

        verify(adsOperations, never()).applyAdToUpcomingTrack(any(ApiAdsForTrack.class));
    }

    @Test
    public void trackChangeEventDoesNothingIfNextTrackIsNotMonetizable() {
        when(playQueueManager.hasNextItem()).thenReturn(true);
        when(trackRepository.track(nextTrackUrn)).thenReturn(Observable.just(nextNonMonetizablePropertySet));
        adsController.subscribe();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(currentTrackUrn), Urn.NOT_SET, 0));

        verify(adsOperations, never()).applyAdToUpcomingTrack(any(ApiAdsForTrack.class));
    }

    @Test
    public void trackChangeEventDoesNothingIfAudioAdFetchEmitsError() {
        when(playQueueManager.hasNextItem()).thenReturn(true);
        when(trackRepository.track(nextTrackUrn)).thenReturn(Observable.just(nextMonetizablePropertySet));
        when(adsOperations.ads(eq(nextTrackUrn), anyBoolean(), anyBoolean())).thenReturn(Observable.<ApiAdsForTrack>error(new IOException("Ad fetch error")));
        adsController.subscribe();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(currentTrackUrn), Urn.NOT_SET, 0));

        verify(adsOperations, never()).applyAdToUpcomingTrack(any(ApiAdsForTrack.class));
    }

    @Test
    public void trackChangeEventClearsAudioAd() {
        adsController.subscribe();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(currentTrackUrn), Urn.NOT_SET, 0));

        verify(adsOperations).clearAllAdsFromQueue();
    }

    @Test
    public void trackChangeEventDoesNotClearAudioAdIfCurrentlyPlayingAd() {
        when(adsOperations.isCurrentItemAd()).thenReturn(true);
        adsController.subscribe();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(currentTrackUrn), Urn.NOT_SET, 0));

        verify(adsOperations, never()).clearAllAdsFromQueue();
    }

    @Test
    public void queueUpdateEventDoesNotClearAudioAd() {
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate(Urn.NOT_SET));

        verify(adsOperations, never()).clearAllAdsFromQueue();
    }

    @Test
    public void trackChangeClearsAllAdsWhenAddingNewAd() {
        when(trackRepository.track(nextTrackUrn)).thenReturn(Observable.just(nextMonetizablePropertySet));
        when(adsOperations.ads(eq(nextTrackUrn), anyBoolean(), anyBoolean())).thenReturn(Observable.just(apiAdsForTrack));
        when(playQueueManager.hasTrackAsNextItem()).thenReturn(true);
        adsController.subscribe();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromPositionChanged(currentPlayQueueItem, Urn.NOT_SET, 0));

        InOrder inOrder = inOrder(adsOperations);
        inOrder.verify(adsOperations).clearAllAdsFromQueue();
        inOrder.verify(adsOperations).applyAdToUpcomingTrack(apiAdsForTrack);
    }

    @Test
    public void unsubscribesFromCurrentAdFetchOnPlayQueueEvent() {
        final PublishSubject<ApiAdsForTrack> adsObservable = PublishSubject.create();

        when(trackRepository.track(currentTrackUrn)).thenReturn(Observable.just(currentMonetizablePropertySet));
        when(adsOperations.ads(eq(currentTrackUrn), anyBoolean(), anyBoolean())).thenReturn(adsObservable);
        adsController.subscribe();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromPositionChanged(currentPlayQueueItem, Urn.NOT_SET, 0));

        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(999L))); // url must be different
        when(playQueueManager.isCurrentItem(currentPlayQueueItem)).thenReturn(false);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(currentTrackUrn), Urn.NOT_SET, 0));

        assertThat(adsObservable.hasObservers()).isFalse();
    }

    @Test
    public void unsubscribesFromNextAdFetchOnPlayQueueEvent() {
        final PublishSubject<ApiAdsForTrack> adsObservable = PublishSubject.create();
        when(playQueueManager.hasNextItem()).thenReturn(true);
        when(trackRepository.track(nextTrackUrn)).thenReturn(Observable.just(nextMonetizablePropertySet));
        when(adsOperations.ads(eq(nextTrackUrn), anyBoolean(), anyBoolean())).thenReturn(adsObservable);
        adsController.subscribe();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(nextTrackUrn), Urn.NOT_SET, 0));

        when(playQueueManager.isNextItem(nextPlayQueueItem)).thenReturn(false);
        when(playQueueManager.getNextPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(999L))); // urn must be different

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(nextTrackUrn), Urn.NOT_SET, 0));

        assertThat(adsObservable.hasObservers()).isFalse();
    }

    @Test
    public void doesNotUnsubscribeFromNextAdFetchOnPlayQueueEventIfNextTrackIsNowCurrentTrack() {
        final PublishSubject<ApiAdsForTrack> adsObservable = PublishSubject.create();
        when(playQueueManager.hasNextItem()).thenReturn(true);
        when(trackRepository.track(nextTrackUrn)).thenReturn(Observable.just(nextMonetizablePropertySet));
        when(adsOperations.ads(eq(nextTrackUrn), anyBoolean(), anyBoolean())).thenReturn(adsObservable);
        adsController.subscribe();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(nextTrackUrn), Urn.NOT_SET, 0));

        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(nextTrackUrn));
        when(playQueueManager.getNextPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(999L)));

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(nextTrackUrn), Urn.NOT_SET, 0));

        assertThat(adsObservable.hasObservers()).isTrue();
    }

    @Test
    public void unsubscribeFromNextAdFetchOnPlayQueueEventIfNextTrackIsNowCurrentTrackAndStale() {
        final PublishSubject<ApiAdsForTrack> adsObservable1 = PublishSubject.create();
        final PublishSubject<ApiAdsForTrack> adsObservable2 = PublishSubject.create();
        when(playQueueManager.hasTrackAsNextItem()).thenReturn(true);
        when(trackRepository.track(nextTrackUrn)).thenReturn(Observable.just(nextMonetizablePropertySet));
        when(adsOperations.ads(eq(nextTrackUrn), anyBoolean(), anyBoolean())).thenReturn(adsObservable1, adsObservable2);

        // cheap override of stale time to avoid a horrible sequence of exposing internal implementation
        adsController = new AdsController(eventBus, adsOperations, visualAdImpressionOperations,
                adOverlayImpressionOperations, playQueueManager, trackRepository, scheduler, -1L);

        adsController.subscribe();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(nextTrackUrn), Urn.NOT_SET, 0));

        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(nextTrackUrn));
        when(playQueueManager.getNextPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(999L)));

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(nextTrackUrn), Urn.NOT_SET, 0));

        assertThat(adsObservable1.hasObservers()).isFalse();
        assertThat(adsObservable2.hasObservers()).isTrue();
    }

    @Test
    public void playStateChangedEventWhenBufferingAndAdAutoAdvancesTrackAfterTimeout() {
        when(adsOperations.isCurrentItemAd()).thenReturn(true);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.buffering());

        verify(playQueueManager, never()).autoMoveToNextPlayableItem();
        scheduler.advanceTimeBy(AdsController.FAILED_AD_WAIT_SECS, TimeUnit.SECONDS);
        verify(playQueueManager).autoMoveToNextPlayableItem();
    }

    @Test
    public void playStateChangedEventWhenBufferingAnAudioAdAutoLogATrackingEvent() {
        when(adsOperations.isCurrentItemAd()).thenReturn(true);
        adsController.subscribe();

        final Urn trackUrn = Urn.forTrack(123L);
        final PlaybackStateTransition stateTransition = new PlaybackStateTransition(PlaybackState.BUFFERING, PlayStateReason.NONE, trackUrn, 12, 1200);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, stateTransition);
        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).isEmpty();

        scheduler.advanceTimeBy(AdsController.FAILED_AD_WAIT_SECS, TimeUnit.SECONDS);

        final AdFailedToBufferEvent event = (AdFailedToBufferEvent) eventBus.eventsOn(EventQueue.TRACKING).get(0);
        assertThat(event.getAttributes().get(AdTrackingKeys.KEY_AD_URN)).isEqualTo(trackUrn.toString());
        assertThat(event.getAttributes().get(AdFailedToBufferEvent.PLAYBACK_POSITION)).isEqualTo("12");
        assertThat(event.getAttributes().get(AdFailedToBufferEvent.WAIT_PERIOD)).isEqualTo("6");
    }

    @Test
    public void playStateChangedEventWhenBufferingAVideoAdAutoLogATrackingEvent() {
        when(adsOperations.isCurrentItemAd()).thenReturn(true);
        adsController.subscribe();

        final Urn videoAdUrn = Urn.forAd("dfp", "video-ad");
        final PlaybackStateTransition stateTransition = new PlaybackStateTransition(PlaybackState.BUFFERING, PlayStateReason.NONE, videoAdUrn, 12, 1200);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, stateTransition);
        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).isEmpty();

        scheduler.advanceTimeBy(AdsController.FAILED_AD_WAIT_SECS, TimeUnit.SECONDS);

        final AdFailedToBufferEvent event = (AdFailedToBufferEvent) eventBus.eventsOn(EventQueue.TRACKING).get(0);
        assertThat(event.getAttributes().get(AdTrackingKeys.KEY_AD_URN)).isEqualTo(videoAdUrn.toString());
        assertThat(event.getAttributes().get(AdFailedToBufferEvent.PLAYBACK_POSITION)).isEqualTo("12");
        assertThat(event.getAttributes().get(AdFailedToBufferEvent.WAIT_PERIOD)).isEqualTo("6");
    }

    @Test
    public void playStateChangedEventForBufferingNormalTrackDoesNotAdvanceAfterTimeout() {
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(false);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.buffering());

        scheduler.advanceTimeBy(AdsController.FAILED_AD_WAIT_SECS, TimeUnit.SECONDS);
        verify(playQueueManager, never()).autoMoveToNextPlayableItem();
    }

    @Test
    public void playEventUnsubscribesFromSkipAd() {
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);
        adsController.subscribe();
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.buffering());

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.playing());
        scheduler.advanceTimeBy(AdsController.FAILED_AD_WAIT_SECS, TimeUnit.SECONDS);

        verify(playQueueManager, never()).autoMoveToNextPlayableItem();
    }

    @Test
    public void pauseEventUnsubscribesFromSkipAd() {
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);
        adsController.subscribe();
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.buffering());

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.idle());
        scheduler.advanceTimeBy(AdsController.FAILED_AD_WAIT_SECS, TimeUnit.SECONDS);

        verify(playQueueManager, never()).autoMoveToNextPlayableItem();
    }

    @Test
    public void trackChangeEventUnsubscribesFromSkipAd() {
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);
        adsController.subscribe();
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.buffering());

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(currentTrackUrn), Urn.NOT_SET, 0));
        scheduler.advanceTimeBy(AdsController.FAILED_AD_WAIT_SECS, TimeUnit.SECONDS);

        verify(playQueueManager, never()).autoMoveToNextPlayableItem();
    }

    @Test
    public void playbackErrorEventCausesAdToBeSkipped() {
        when(adsOperations.isCurrentItemAd()).thenReturn(true);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.error(PlayStateReason.ERROR_FAILED));

        verify(playQueueManager).autoMoveToNextPlayableItem();
    }

    @Test
    public void playbackErrorDoesNotSkipNormalTrack() {
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(false);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.error(PlayStateReason.ERROR_FAILED));

        verify(playQueueManager, never()).autoMoveToNextPlayableItem();
    }

    @Test
    public void playStateChangeEventForAudioAdEndingSetsUpLeaveBehind() throws Exception {
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);
        final LeaveBehindAd monetizableAdData = AdFixtures.getLeaveBehindAd(Urn.forTrack(123L));
        when(adsOperations.getNextTrackAdData()).thenReturn(Optional.<AdData>of(monetizableAdData));

        adsController.onPlayStateTransition(TestPlayStates.complete());

        assertThat(monetizableAdData.isMetaAdCompleted()).isTrue();
    }

    @Test
    public void shouldPublishTrackingEventWhenVisualAdControllerEmitsEvent() {
        TrackingEvent trackingEvent = TestEvents.unspecifiedTrackingEvent();
        when(visualAdImpressionOperations.trackImpression()).thenReturn(Observable.just(trackingEvent));

        adsController.subscribe();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isEqualTo(trackingEvent);
    }

    @Test
    public void shouldPublishTrackingEventWhenLeaveBehindControllerEmitsEvent() {
        TrackingEvent trackingEvent = TestEvents.unspecifiedTrackingEvent();
        when(adOverlayImpressionOperations.trackImpression()).thenReturn(Observable.just(trackingEvent));

        adsController.subscribe();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isEqualTo(trackingEvent);
    }

    @Test
    public void publishAdDeliveryEventShouldPublishEventIfAdForNextTrackExists() {
        insertFullAdsForNextTrack();
        when(playQueueManager.getUpcomingPlayQueueItems(anyInt())).thenReturn(Arrays.asList(nextTrackUrn));
        when(adsOperations.getNextTrackAdData()).thenReturn(Optional.<AdData>of(AudioAd.create(apiAdsForTrack.audioAd().get(), currentTrackUrn)));

        adsController.publishAdDeliveryEventIfUpcoming();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).isEqualTo(AdDeliveryEvent.AD_DELIVERED_KIND);
    }

    @Test
    public void publishAdDeliveryEventShouldNotPublishEventIfNoAdForNextTrackExists() {
        when(playQueueManager.getUpcomingPlayQueueItems(anyInt())).thenReturn(Arrays.asList(nextTrackUrn));
        when(adsOperations.getNextTrackAdData()).thenReturn(Optional.<AdData>of(AudioAd.create(apiAdsForTrack.audioAd().get(), currentTrackUrn)));

        adsController.publishAdDeliveryEventIfUpcoming();

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).isEmpty();
    }

    @Test
    public void publishAdDeliveryEventShouldNotPublishEventIfAdsMonetizableTrackCantBeFound() {
        insertFullAdsForNextTrack();
        when(playQueueManager.getUpcomingPlayQueueItems(anyInt())).thenReturn(Collections.<Urn>emptyList());
        when(adsOperations.getNextTrackAdData()).thenReturn(Optional.<AdData>of(AudioAd.create(apiAdsForTrack.audioAd().get(), currentTrackUrn)));

        adsController.publishAdDeliveryEventIfUpcoming();

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).isEmpty();
    }

    private void insertFullAdsForNextTrack() {
        when(playQueueManager.hasTrackAsNextItem()).thenReturn(true);
        when(trackRepository.track(nextTrackUrn)).thenReturn(Observable.just(nextMonetizablePropertySet));
        when(adsOperations.ads(eq(nextTrackUrn), anyBoolean(), anyBoolean())).thenReturn(Observable.just(apiAdsForTrack));
        adsController.subscribe();
        final TrackQueueItem trackItem = TestPlayQueueItem.createTrack(currentTrackUrn, AudioAd.create(apiAdsForTrack.audioAd().get(), currentTrackUrn));
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(trackItem, Urn.NOT_SET, 0));
    }

}
