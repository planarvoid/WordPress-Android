package com.soundcloud.android.ads;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playback.service.Playa.PlayaState;
import static com.soundcloud.android.playback.service.Playa.Reason;
import static com.soundcloud.android.playback.service.Playa.StateTransition;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.AdFixtures;
import com.soundcloud.android.testsupport.fixtures.TestEvents;
import com.soundcloud.android.tracks.TrackOperations;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Subscription;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RunWith(SoundCloudTestRunner.class)
public class AdsControllerTest {

    private static final Urn CURRENT_TRACK_URN = Urn.forTrack(122L);
    private static final Urn NEXT_TRACK_URN = Urn.forTrack(123L);
    private static final PropertySet MONETIZABLE_PROPERTY_SET = PropertySet.from(TrackProperty.URN.bind(NEXT_TRACK_URN), TrackProperty.MONETIZABLE.bind(true));
    private static final PropertySet NON_MONETIZABLE_PROPERTY_SET = PropertySet.from(TrackProperty.URN.bind(NEXT_TRACK_URN), TrackProperty.MONETIZABLE.bind(false));

    @Mock private PlayQueueManager playQueueManager;
    @Mock private AdsOperations adsOperations;
    @Mock private TrackOperations trackOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private VisualAdImpressionOperations visualAdImpressionOperations;
    @Mock private LeaveBehindImpressionOperations leaveBehindImpressionOperations;
    @Mock private FeatureFlags featureFlags;

    private TestEventBus eventBus = new TestEventBus();
    private TestScheduler scheduler = Schedulers.test();
    private TestSubscriber<Playa.StateTransition> leaveBehindSubscriber = new TestSubscriber<>();
    private ApiAdsForTrack apiAdsForTrack;
    private AdsController adsController;

    @Before
    public void setUp() throws Exception {
        when(visualAdImpressionOperations.trackImpression()).thenReturn(Observable.<TrackingEvent>never());
        when(leaveBehindImpressionOperations.trackImpression()).thenReturn(Observable.<TrackingEvent>never());

        adsController = new AdsController(eventBus, adsOperations, visualAdImpressionOperations, leaveBehindImpressionOperations,
                playQueueManager, trackOperations, featureFlags, scheduler);
        apiAdsForTrack = AdFixtures.fullAdsForTrack();
    }

    @Test
    public void trackChangeEventInsertsAudioAdIntoPlayQueue() throws CreateModelException {
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(Observable.just(MONETIZABLE_PROPERTY_SET));
        when(adsOperations.ads(NEXT_TRACK_URN)).thenReturn(Observable.just(apiAdsForTrack));

        adsController.subscribe();
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN, apiAdsForTrack.audioAd().toPropertySet()));

        verify(adsOperations).applyAdToTrack(NEXT_TRACK_URN, apiAdsForTrack);
    }

    @Test
    public void trackChangeEventDoesNotFetchTrackFromStorageIfAlreadyTryingToFetchAd() throws CreateModelException {
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(TestObservables.<PropertySet>endlessObservablefromSubscription(Mockito.mock(Subscription.class)));
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate());
        verify(trackOperations).track(any(Urn.class));
    }


    @Test
    public void playQueueUpdateEventInsertsAudioAdIntoPlayQueue() throws CreateModelException {
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(Observable.just(MONETIZABLE_PROPERTY_SET));
        when(adsOperations.ads(NEXT_TRACK_URN)).thenReturn(Observable.just(apiAdsForTrack));
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate());

        verify(adsOperations).applyAdToTrack(NEXT_TRACK_URN, apiAdsForTrack);
    }

    @Test
    public void newQueueEventDoesNotInsertAudioAdIntoPlayQueue() throws CreateModelException {
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(any(Urn.class))).thenReturn(Observable.just(MONETIZABLE_PROPERTY_SET));
        when(adsOperations.ads(any(Urn.class))).thenReturn(Observable.just(apiAdsForTrack));
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue());

        verify(adsOperations, never()).applyAdToTrack(any(Urn.class), any(ApiAdsForTrack.class));
    }

    @Test
    public void trackChangeEventDoesNothingIfNoNextTrack() {
        when(playQueueManager.hasNextTrack()).thenReturn(false);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));

        verify(adsOperations, never()).applyAdToTrack(any(Urn.class), any(ApiAdsForTrack.class));
    }

    @Test
    public void trackChangeEventDoesNothingIfNextTrackIsAudioAd() {
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(Observable.just(MONETIZABLE_PROPERTY_SET));
        when(adsOperations.ads(NEXT_TRACK_URN)).thenReturn(Observable.just(apiAdsForTrack));
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(adsOperations.isNextTrackAudioAd()).thenReturn(true);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));

        verify(adsOperations, never()).applyAdToTrack(any(Urn.class), any(ApiAdsForTrack.class));
    }

    @Test
    public void trackChangeEventDoesNothingIfAlreadyPlayingAd() {
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(Observable.just(MONETIZABLE_PROPERTY_SET));
        when(adsOperations.ads(NEXT_TRACK_URN)).thenReturn(Observable.just(apiAdsForTrack));
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));

        verify(adsOperations, never()).applyAdToTrack(any(Urn.class), any(ApiAdsForTrack.class));
    }

    @Test
    public void trackChangeEventDoesNothingIfNextTrackIsNotMonetizable() {
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(Observable.just(NON_MONETIZABLE_PROPERTY_SET));
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));

        verify(adsOperations, never()).applyAdToTrack(any(Urn.class), any(ApiAdsForTrack.class));
    }

    @Test
    public void trackChangeEventDoesNothingIfAudioAdFetchEmitsError() {
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(Observable.just(MONETIZABLE_PROPERTY_SET));
        when(adsOperations.ads(NEXT_TRACK_URN)).thenReturn(Observable.<ApiAdsForTrack>error(new IOException("Ad fetch error")));
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));

        verify(adsOperations, never()).applyAdToTrack(any(Urn.class), any(ApiAdsForTrack.class));
    }

    @Test
    public void trackChangeEventClearsAudioAd() {
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));

        verify(adsOperations).clearAllAds();
    }

    @Test
    public void trackChangeEventDoesNotClearAudioAdIfCurrentlyPlayingAd() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));

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
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(Observable.just(MONETIZABLE_PROPERTY_SET));
        when(adsOperations.ads(NEXT_TRACK_URN)).thenReturn(Observable.just(apiAdsForTrack));
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));

        InOrder inOrder = inOrder(adsOperations);
        inOrder.verify(adsOperations).clearAllAds();
        inOrder.verify(adsOperations).applyAdToTrack(NEXT_TRACK_URN, apiAdsForTrack);
    }

    @Test
    public void unsubscribesFromCurrentAdFetchOnPlayQueueEvent() {
        Subscription adFetchSubscription = mock(Subscription.class);
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(Observable.just(MONETIZABLE_PROPERTY_SET));
        when(adsOperations.ads(NEXT_TRACK_URN)).thenReturn(TestObservables.<ApiAdsForTrack>endlessObservablefromSubscription(adFetchSubscription));
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));

        verify(adFetchSubscription).unsubscribe();
    }

    @Test
    public void playStateChangedEventWhenBufferingAndAdAutoAdvancesTrackAfterTimeout() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(PlayaState.BUFFERING, Reason.NONE));

        verify(playQueueManager, never()).autoNextTrack();
        scheduler.advanceTimeBy(AdsController.SKIP_DELAY_SECS, TimeUnit.SECONDS);
        verify(playQueueManager).autoNextTrack();
    }

    @Test
    public void playStateChangedEventForBufferingNormalTrackDoesNotAdvanceAfterTimeout() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(false);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(PlayaState.BUFFERING, Reason.NONE));

        scheduler.advanceTimeBy(AdsController.SKIP_DELAY_SECS, TimeUnit.SECONDS);
        verify(playQueueManager, never()).autoNextTrack();
    }

    @Test
    public void playEventUnsubscribesFromSkipAd() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        adsController.subscribe();
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(PlayaState.BUFFERING, Reason.NONE));

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(PlayaState.PLAYING, Reason.NONE));
        scheduler.advanceTimeBy(AdsController.SKIP_DELAY_SECS, TimeUnit.SECONDS);

        verify(playQueueManager, never()).autoNextTrack();
    }

    @Test
    public void pauseEventUnsubscribesFromSkipAd() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        adsController.subscribe();
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(PlayaState.BUFFERING, Reason.NONE));

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(PlayaState.IDLE, Reason.NONE));
        scheduler.advanceTimeBy(AdsController.SKIP_DELAY_SECS, TimeUnit.SECONDS);

        verify(playQueueManager, never()).autoNextTrack();
    }

    @Test
    public void trackChangeEventUnsubscribesFromSkipAd() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        adsController.subscribe();
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(PlayaState.BUFFERING, Reason.NONE));

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));
        scheduler.advanceTimeBy(AdsController.SKIP_DELAY_SECS, TimeUnit.SECONDS);

        verify(playQueueManager, never()).autoNextTrack();
    }

    @Test
    public void playbackErrorEventCausesAdToBeSkipped() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(PlayaState.IDLE, Reason.ERROR_FAILED));

        verify(playQueueManager).autoNextTrack();
    }

    @Test
    public void playbackErrorDoesNotSkipNormalTrack() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(false);
        adsController.subscribe();

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(PlayaState.IDLE, Reason.ERROR_FAILED));

        verify(playQueueManager, never()).autoNextTrack();
    }

    @Test
    public void playStateChangeEventForAudioAdEndingSetsUpLeaveBehind() throws Exception {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        final PropertySet monetizableProperties = PropertySet.create();
        when(adsOperations.getMonetizableTrackMetaData()).thenReturn(monetizableProperties);

        adsController.subscribe();

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(PlayaState.IDLE, Reason.TRACK_COMPLETE));

        expect(monetizableProperties.get(LeaveBehindProperty.META_AD_COMPLETED)).toBeTrue();
    }

    @Test
    public void shouldPublishTrackingEventWhenVisualAdControllerEmitsEvent() {
        TrackingEvent trackingEvent = TestEvents.unspecifiedTrackingEvent();
        when(visualAdImpressionOperations.trackImpression()).thenReturn(Observable.just(trackingEvent));

        adsController.subscribe();

        expect(eventBus.lastEventOn(EventQueue.TRACKING)).toEqual(trackingEvent);
    }

    @Test
    public void shouldPublishTrackingEventWhenLeaveBehindControllerEmitsEvent() {
        when(featureFlags.isEnabled(Feature.LEAVE_BEHIND)).thenReturn(true);
        TrackingEvent trackingEvent = TestEvents.unspecifiedTrackingEvent();
        when(leaveBehindImpressionOperations.trackImpression()).thenReturn(Observable.just(trackingEvent));

        adsController.subscribe();

        expect(eventBus.lastEventOn(EventQueue.TRACKING)).toEqual(trackingEvent);
    }
}
