package com.soundcloud.android.ads;

import static com.soundcloud.android.playback.Playa.PlayaState;
import static com.soundcloud.android.playback.Playa.Reason;
import static com.soundcloud.android.playback.Playa.StateTransition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.AdTrackingKeys;
import com.soundcloud.android.events.AudioAdFailedToBufferEvent;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestEvents;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AdsControllerTest extends AndroidUnitTest {

    private static final Urn CURRENT_TRACK_URN = Urn.forTrack(122L);
    private static final Urn NEXT_TRACK_URN = Urn.forTrack(123L);

    private static final PropertySet CURRENT_MONETIZABLE_PROPERTY_SET = PropertySet.from(TrackProperty.URN.bind(CURRENT_TRACK_URN), TrackProperty.MONETIZABLE.bind(true));
    private static final PropertySet NEXT_TRACK_MONETIZABLE_PROPERTY_SET = PropertySet.from(TrackProperty.URN.bind(NEXT_TRACK_URN), TrackProperty.MONETIZABLE.bind(true));
    private static final PropertySet NEXT_TRACK_NON_MONETIZABLE_PROPERTY_SET = PropertySet.from(TrackProperty.URN.bind(NEXT_TRACK_URN), TrackProperty.MONETIZABLE.bind(false));


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
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(NEXT_TRACK_NON_MONETIZABLE_PROPERTY_SET));

        when(playQueueManager.getCurrentTrackUrn()).thenReturn(CURRENT_TRACK_URN);
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);

        adsController = new AdsController(eventBus, adsOperations, visualAdImpressionOperations, adOverlayImpressionOperations,
                playQueueManager, trackRepository, scheduler);
        apiAdsForTrack = AdFixtures.fullAdsForTrack();
    }

    @Test
    public void trackChangeEventInsertsAudioAdIntoPlayQueue() throws CreateModelException {
        when(playQueueManager.hasNextTrack()).thenReturn(true);


        when(trackRepository.track(NEXT_TRACK_URN)).thenReturn(Observable.just(NEXT_TRACK_MONETIZABLE_PROPERTY_SET));
        when(adsOperations.ads(NEXT_TRACK_URN)).thenReturn(Observable.just(apiAdsForTrack));


        adsController.subscribe();
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN, Urn.NOT_SET, apiAdsForTrack.audioAd().toPropertySet(), 0));

        verify(adsOperations).applyAdToTrack(NEXT_TRACK_URN, apiAdsForTrack);
    }

    @Test
    public void trackChangeEventInsertsInterstitialForCurrentTrackIntoPlayQueue() throws CreateModelException {
        when(trackRepository.track(CURRENT_TRACK_URN)).thenReturn(Observable.just(CURRENT_MONETIZABLE_PROPERTY_SET));
        when(adsOperations.ads(CURRENT_TRACK_URN)).thenReturn(Observable.just(apiAdsForTrack));

        adsController.subscribe();
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN, Urn.NOT_SET, apiAdsForTrack.audioAd().toPropertySet(), 0));

        verify(adsOperations).applyInterstitialToTrack(CURRENT_TRACK_URN, apiAdsForTrack);
    }

    @Test
    public void trackChangeEventDoesNotFetchTrackFromStorageIfAlreadyTryingToFetchAd() throws CreateModelException {
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(trackRepository.track(NEXT_TRACK_URN)).thenReturn(Observable.<PropertySet>empty());
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(NEXT_TRACK_URN, Urn.NOT_SET, 0));
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate());
        verify(trackRepository).track(NEXT_TRACK_URN);
    }


    @Test
    public void playQueueUpdateEventInsertsAudioAdIntoPlayQueue() throws CreateModelException {
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(trackRepository.track(NEXT_TRACK_URN)).thenReturn(Observable.just(NEXT_TRACK_MONETIZABLE_PROPERTY_SET));
        when(adsOperations.ads(NEXT_TRACK_URN)).thenReturn(Observable.just(apiAdsForTrack));
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate());

        verify(adsOperations).applyAdToTrack(NEXT_TRACK_URN, apiAdsForTrack);
    }

    @Test
    public void newQueueEventDoesNotInsertAudioAdIntoPlayQueue() throws CreateModelException {
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(NEXT_TRACK_MONETIZABLE_PROPERTY_SET));
        when(adsOperations.ads(any(Urn.class))).thenReturn(Observable.just(apiAdsForTrack));
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue());

        verify(adsOperations, never()).applyAdToTrack(any(Urn.class), any(ApiAdsForTrack.class));
    }

    @Test
    public void trackChangeEventDoesNothingIfNoNextTrack() {
        when(playQueueManager.hasNextTrack()).thenReturn(false);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN, Urn.NOT_SET, 0));

        verify(adsOperations, never()).applyAdToTrack(any(Urn.class), any(ApiAdsForTrack.class));
    }

    @Test
    public void trackChangeEventDoesNothingIfNextTrackIsAudioAd() {
        when(trackRepository.track(NEXT_TRACK_URN)).thenReturn(Observable.just(NEXT_TRACK_MONETIZABLE_PROPERTY_SET));
        when(adsOperations.ads(NEXT_TRACK_URN)).thenReturn(Observable.just(apiAdsForTrack));
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(adsOperations.isNextTrackAudioAd()).thenReturn(true);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN, Urn.NOT_SET, 0));

        verify(adsOperations, never()).applyAdToTrack(any(Urn.class), any(ApiAdsForTrack.class));
    }

    @Test
    public void trackChangeEventDoesNotApplyAdIfAlreadyPlayingAd() {
        when(trackRepository.track(NEXT_TRACK_URN)).thenReturn(Observable.just(NEXT_TRACK_MONETIZABLE_PROPERTY_SET));
        when(adsOperations.ads(NEXT_TRACK_URN)).thenReturn(Observable.just(apiAdsForTrack));
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN, Urn.NOT_SET, 0));

        verify(adsOperations, never()).applyAdToTrack(any(Urn.class), any(ApiAdsForTrack.class));
    }

    @Test
    public void trackChangeEventDoesNothingIfNextTrackIsNotMonetizable() {
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(trackRepository.track(NEXT_TRACK_URN)).thenReturn(Observable.just(NEXT_TRACK_NON_MONETIZABLE_PROPERTY_SET));
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN, Urn.NOT_SET, 0));

        verify(adsOperations, never()).applyAdToTrack(any(Urn.class), any(ApiAdsForTrack.class));
    }

    @Test
    public void trackChangeEventDoesNothingIfAudioAdFetchEmitsError() {
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(trackRepository.track(NEXT_TRACK_URN)).thenReturn(Observable.just(NEXT_TRACK_MONETIZABLE_PROPERTY_SET));
        when(adsOperations.ads(NEXT_TRACK_URN)).thenReturn(Observable.<ApiAdsForTrack>error(new IOException("Ad fetch error")));
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN, Urn.NOT_SET, 0));

        verify(adsOperations, never()).applyAdToTrack(any(Urn.class), any(ApiAdsForTrack.class));
    }

    @Test
    public void trackChangeEventClearsAudioAd() {
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN, Urn.NOT_SET, 0));

        verify(adsOperations).clearAllAds();
    }

    @Test
    public void trackChangeEventDoesNotClearAudioAdIfCurrentlyPlayingAd() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN, Urn.NOT_SET, 0));

        verify(adsOperations, never()).clearAllAds();
    }

    @Test
    public void queueUpdateEventDoesNotClearAudioAd() {
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate());

        verify(adsOperations, never()).clearAllAds();
    }

    @Test
    public void trackChangeClearsAllAdsWhenAddingNewAd() {
        when(trackRepository.track(NEXT_TRACK_URN)).thenReturn(Observable.just(NEXT_TRACK_MONETIZABLE_PROPERTY_SET));
        when(adsOperations.ads(NEXT_TRACK_URN)).thenReturn(Observable.just(apiAdsForTrack));
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN, Urn.NOT_SET, 0));

        InOrder inOrder = inOrder(adsOperations);
        inOrder.verify(adsOperations).clearAllAds();
        inOrder.verify(adsOperations).applyAdToTrack(NEXT_TRACK_URN, apiAdsForTrack);
    }

    @Test
    public void unsubscribesFromCurrentAdFetchOnPlayQueueEvent() {
        final PublishSubject<ApiAdsForTrack> adsObservable = PublishSubject.create();

        when(trackRepository.track(CURRENT_TRACK_URN)).thenReturn(Observable.just(CURRENT_MONETIZABLE_PROPERTY_SET));
        when(adsOperations.ads(CURRENT_TRACK_URN)).thenReturn(adsObservable);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN, Urn.NOT_SET, 0));
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(Urn.forTrack(999L)); // url must be different
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN, Urn.NOT_SET, 0));

        assertThat(adsObservable.hasObservers()).isFalse();
    }

    @Test
    public void unsubscribesFromNextAdFetchOnPlayQueueEvent() {
        final PublishSubject<ApiAdsForTrack> adsObservable = PublishSubject.create();
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(trackRepository.track(NEXT_TRACK_URN)).thenReturn(Observable.just(NEXT_TRACK_MONETIZABLE_PROPERTY_SET));

        when(adsOperations.ads(NEXT_TRACK_URN)).thenReturn(adsObservable);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(NEXT_TRACK_URN, Urn.NOT_SET, 0));
        when(playQueueManager.getNextTrackUrn()).thenReturn(Urn.forTrack(999L));
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(NEXT_TRACK_URN, Urn.NOT_SET, 0));

        assertThat(adsObservable.hasObservers()).isFalse();
    }

    @Test
    public void doesNotUnsubscribeFromNextAdFetchOnPlayQueueEventIfNextTrackIsNowCurrentTrack() {
        final PublishSubject<ApiAdsForTrack> adsObservable = PublishSubject.create();
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(trackRepository.track(NEXT_TRACK_URN)).thenReturn(Observable.just(NEXT_TRACK_MONETIZABLE_PROPERTY_SET));
        when(adsOperations.ads(NEXT_TRACK_URN)).thenReturn(adsObservable);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(NEXT_TRACK_URN, Urn.NOT_SET, 0));

        when(playQueueManager.getCurrentTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(playQueueManager.getNextTrackUrn()).thenReturn(Urn.forTrack(999L));
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(NEXT_TRACK_URN, Urn.NOT_SET, 0));

        assertThat(adsObservable.hasObservers()).isTrue();
    }

    @Test
    public void unsubscribeFromNextAdFetchOnPlayQueueEventIfNextTrackIsNowCurrentTrackAndStale() {
        final PublishSubject<ApiAdsForTrack> adsObservable1 = PublishSubject.create();
        final PublishSubject<ApiAdsForTrack> adsObservable2 = PublishSubject.create();
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(trackRepository.track(NEXT_TRACK_URN)).thenReturn(Observable.just(NEXT_TRACK_MONETIZABLE_PROPERTY_SET));
        when(adsOperations.ads(NEXT_TRACK_URN)).thenReturn(adsObservable1, adsObservable2);

        // cheap override of stale time to avoid a horrible sequence of exposing internal implementation
        adsController = new AdsController(eventBus, adsOperations, visualAdImpressionOperations,
                adOverlayImpressionOperations, playQueueManager, trackRepository, scheduler, -1L);

        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(NEXT_TRACK_URN, Urn.NOT_SET, 0));

        when(playQueueManager.getCurrentTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(playQueueManager.getNextTrackUrn()).thenReturn(Urn.forTrack(999L));
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(NEXT_TRACK_URN, Urn.NOT_SET, 0));

        assertThat(adsObservable1.hasObservers()).isFalse();
        assertThat(adsObservable2.hasObservers()).isTrue();
    }

    @Test
    public void playStateChangedEventWhenBufferingAndAdAutoAdvancesTrackAfterTimeout() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.buffering());

        verify(playQueueManager, never()).autoNextTrack();
        scheduler.advanceTimeBy(AdsController.FAILED_AD_WAIT_SECS, TimeUnit.SECONDS);
        verify(playQueueManager).autoNextTrack();
    }

    @Test
    public void playStateChangedEventWhenBufferingAndAdAutoLogATrackingEvent() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        adsController.subscribe();

        final StateTransition stateTransition = new StateTransition(PlayaState.BUFFERING, Reason.NONE, new Urn("provider:ad:345"), 12, 1200);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, stateTransition);
        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).isEmpty();

        scheduler.advanceTimeBy(AdsController.FAILED_AD_WAIT_SECS, TimeUnit.SECONDS);

        final AudioAdFailedToBufferEvent event = (AudioAdFailedToBufferEvent) eventBus.eventsOn(EventQueue.TRACKING).get(0);
        assertThat(event.getAttributes().get(AdTrackingKeys.KEY_AD_URN)).isEqualTo("provider:ad:345");
        assertThat(event.getAttributes().get(AudioAdFailedToBufferEvent.PLAYBACK_POSITION)).isEqualTo("12");
        assertThat(event.getAttributes().get(AudioAdFailedToBufferEvent.WAIT_PERIOD)).isEqualTo("6");
    }

    @Test
    public void playStateChangedEventForBufferingNormalTrackDoesNotAdvanceAfterTimeout() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(false);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.buffering());

        scheduler.advanceTimeBy(AdsController.FAILED_AD_WAIT_SECS, TimeUnit.SECONDS);
        verify(playQueueManager, never()).autoNextTrack();
    }

    @Test
    public void playEventUnsubscribesFromSkipAd() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        adsController.subscribe();
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.buffering());

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.playing());
        scheduler.advanceTimeBy(AdsController.FAILED_AD_WAIT_SECS, TimeUnit.SECONDS);

        verify(playQueueManager, never()).autoNextTrack();
    }

    @Test
    public void pauseEventUnsubscribesFromSkipAd() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        adsController.subscribe();
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.buffering());

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.idle());
        scheduler.advanceTimeBy(AdsController.FAILED_AD_WAIT_SECS, TimeUnit.SECONDS);

        verify(playQueueManager, never()).autoNextTrack();
    }

    @Test
    public void trackChangeEventUnsubscribesFromSkipAd() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        adsController.subscribe();
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.buffering());

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN, Urn.NOT_SET, 0));
        scheduler.advanceTimeBy(AdsController.FAILED_AD_WAIT_SECS, TimeUnit.SECONDS);

        verify(playQueueManager, never()).autoNextTrack();
    }

    @Test
    public void playbackErrorEventCausesAdToBeSkipped() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.error(Reason.ERROR_FAILED));

        verify(playQueueManager).autoNextTrack();
    }

    @Test
    public void playbackErrorDoesNotSkipNormalTrack() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(false);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.error(Reason.ERROR_FAILED));

        verify(playQueueManager, never()).autoNextTrack();
    }

    @Test
    public void playStateChangeEventForAudioAdEndingSetsUpLeaveBehind() throws Exception {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        final PropertySet monetizableProperties = PropertySet.create();
        when(adsOperations.getMonetizableTrackMetaData()).thenReturn(monetizableProperties);

        adsController.subscribe();

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.complete());

        assertThat(monetizableProperties.get(LeaveBehindProperty.META_AD_COMPLETED)).isTrue();
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
}
