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
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.tracks.TrackOperations;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackUrn;
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
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RunWith(SoundCloudTestRunner.class)
public class AdsControllerTest {

    private static final TrackUrn CURRENT_TRACK_URN = Urn.forTrack(122L);
    private static final TrackUrn NEXT_TRACK_URN = Urn.forTrack(123L);
    private static final PropertySet MONETIZABLE_PROPERTY_SET = PropertySet.from(TrackProperty.URN.bind(NEXT_TRACK_URN), TrackProperty.MONETIZABLE.bind(true));
    private static final PropertySet NON_MONETIZABLE_PROPERTY_SET = PropertySet.from(TrackProperty.URN.bind(NEXT_TRACK_URN), TrackProperty.MONETIZABLE.bind(false));

    @Mock private PlayQueueManager playQueueManager;
    @Mock private AdsOperations adsOperations;
    @Mock private TrackOperations trackOperations;
    @Mock private AccountOperations accountOperations;

    private TestEventBus eventBus = new TestEventBus();
    private TestScheduler scheduler = Schedulers.test();
    private AudioAd audioAd;

    @Before
    public void setUp() throws Exception {

        AdsController adsController = new AdsController(eventBus, adsOperations, playQueueManager, trackOperations, scheduler);
        adsController.subscribe();
        audioAd = TestHelper.getModelFactory().createModel(AudioAd.class);
    }

    @Test
    public void trackChangeEventInsertsAudioAdIntoPlayQueue() throws CreateModelException {
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(Observable.just(MONETIZABLE_PROPERTY_SET));
        when(adsOperations.audioAd(NEXT_TRACK_URN)).thenReturn(Observable.just(audioAd));

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));

        verify(playQueueManager).insertAudioAd(audioAd);
    }

    @Test
    public void trackChangeEventDoesNotFetchTrackFromStorageIfAlreadyTryingToFetchAd() throws CreateModelException {
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(TestObservables.<PropertySet>endlessObservablefromSubscription(Mockito.mock(Subscription.class)));

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate());
        verify(trackOperations).track(any(TrackUrn.class));
    }


    @Test
    public void playQueueUpdateEventInsertsAudioAdIntoPlayQueue() throws CreateModelException {
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(Observable.just(MONETIZABLE_PROPERTY_SET));
        when(adsOperations.audioAd(NEXT_TRACK_URN)).thenReturn(Observable.just(audioAd));

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate());

        verify(playQueueManager).insertAudioAd(audioAd);
    }

    @Test
    public void newQueueEventDoesNotInsertAudioAdIntoPlayQueue() throws CreateModelException {
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(any(TrackUrn.class))).thenReturn(Observable.just(MONETIZABLE_PROPERTY_SET));
        when(adsOperations.audioAd(any(TrackUrn.class))).thenReturn(Observable.just(audioAd));

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue());

        verify(playQueueManager, never()).insertAudioAd(audioAd);
    }

    @Test
    public void trackChangeEventDoesNothingIfNoNextTrack() {
        when(playQueueManager.hasNextTrack()).thenReturn(false);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));

        verify(playQueueManager, never()).insertAudioAd(any(AudioAd.class));
    }

    @Test
    public void trackChangeEventDoesNothingIfNextTrackIsAudioAd() {
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(Observable.just(MONETIZABLE_PROPERTY_SET));
        when(adsOperations.audioAd(NEXT_TRACK_URN)).thenReturn(Observable.just(audioAd));
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(playQueueManager.isNextTrackAudioAd()).thenReturn(true);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));

        verify(playQueueManager, never()).insertAudioAd(any(AudioAd.class));
    }

    @Test
    public void trackChangeEventDoesNothingIfAlreadyPlayingAd() {
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(Observable.just(MONETIZABLE_PROPERTY_SET));
        when(adsOperations.audioAd(NEXT_TRACK_URN)).thenReturn(Observable.just(audioAd));
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));

        verify(playQueueManager, never()).insertAudioAd(any(AudioAd.class));
    }

    @Test
    public void trackChangeEventDoesNothingIfNextTrackIsNotMonetizable() {
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(Observable.just(NON_MONETIZABLE_PROPERTY_SET));

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));

        verify(playQueueManager, never()).insertAudioAd(any(AudioAd.class));
    }

    @Test
    public void trackChangeEventDoesNothingIfAudioAdFetchEmitsError() {
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(Observable.just(MONETIZABLE_PROPERTY_SET));
        when(adsOperations.audioAd(NEXT_TRACK_URN)).thenReturn(Observable.<AudioAd>error(new IOException("Ad fetch error")));

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));

        verify(playQueueManager, never()).insertAudioAd(any(AudioAd.class));
    }

    @Test
    public void trackChangeEventClearsAudioAd() {
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));

        verify(playQueueManager).clearAudioAd();
    }

    @Test
    public void queueUpdateEventDoesNotClearAudioAd() {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate());

        verify(playQueueManager, never()).clearAudioAd();
    }

    @Test
    public void trackChangeClearsAllAdsWhenAddingNewAd() {
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(Observable.just(MONETIZABLE_PROPERTY_SET));
        when(adsOperations.audioAd(NEXT_TRACK_URN)).thenReturn(Observable.just(audioAd));
        when(playQueueManager.hasNextTrack()).thenReturn(true);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));

        InOrder inOrder = inOrder(playQueueManager);
        inOrder.verify(playQueueManager).clearAudioAd();
        inOrder.verify(playQueueManager).insertAudioAd(any(AudioAd.class));
    }

    @Test
    public void unsubscribesFromCurrentAdFetchOnPlayQueueEvent() {
        Subscription adFetchSubscription = mock(Subscription.class);

        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(Observable.just(MONETIZABLE_PROPERTY_SET));
        when(adsOperations.audioAd(NEXT_TRACK_URN)).thenReturn(TestObservables.<AudioAd>endlessObservablefromSubscription(adFetchSubscription));

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));

        verify(adFetchSubscription).unsubscribe();
    }

    @Test
    public void playStateChangedEventWhenBufferingAndAdAutoAdvancesTrackAfterTimeout() {
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(PlayaState.BUFFERING, Reason.NONE));

        verify(playQueueManager, never()).autoNextTrack();
        scheduler.advanceTimeBy(AdsController.SKIP_DELAY_SECS, TimeUnit.SECONDS);
        verify(playQueueManager).autoNextTrack();
    }

    @Test
    public void playStateChangedEventForBufferingNormalTrackDoesNotAdvanceAfterTimeout() {
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(false);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(PlayaState.BUFFERING, Reason.NONE));

        scheduler.advanceTimeBy(AdsController.SKIP_DELAY_SECS, TimeUnit.SECONDS);
        verify(playQueueManager, never()).autoNextTrack();
    }

    @Test
    public void playEventUnsubscribesFromSkipAd() {
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(PlayaState.BUFFERING, Reason.NONE));

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(PlayaState.PLAYING, Reason.NONE));
        scheduler.advanceTimeBy(AdsController.SKIP_DELAY_SECS, TimeUnit.SECONDS);

        verify(playQueueManager, never()).autoNextTrack();
    }

    @Test
    public void pauseEventUnsubscribesFromSkipAd() {
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(PlayaState.BUFFERING, Reason.NONE));

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(PlayaState.IDLE, Reason.NONE));
        scheduler.advanceTimeBy(AdsController.SKIP_DELAY_SECS, TimeUnit.SECONDS);

        verify(playQueueManager, never()).autoNextTrack();
    }

    @Test
    public void trackChangeEventUnsubscribesFromSkipAd() {
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(PlayaState.BUFFERING, Reason.NONE));

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));
        scheduler.advanceTimeBy(AdsController.SKIP_DELAY_SECS, TimeUnit.SECONDS);

        verify(playQueueManager, never()).autoNextTrack();
    }

    @Test
    public void playbackErrorEventCausesAdToBeSkipped() {
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(PlayaState.IDLE, Reason.ERROR_FAILED));

        verify(playQueueManager).autoNextTrack();
    }

    @Test
    public void playbackErrorDoesNotSkipNormalTrack() {
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(false);

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(PlayaState.IDLE, Reason.ERROR_FAILED));

        verify(playQueueManager, never()).autoNextTrack();
    }

    @Test
    public void sendExpandPlayerEventWhenCurrentTrackIsAudioAdOnPlayQueueEvent() {
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));

        expect(eventBus.lastEventOn(EventQueue.PLAYER_UI).getKind()).toEqual(PlayerUIEvent.EXPAND_PLAYER);
    }

    @Test
    public void doNotSendExpandPlayerEventWhenCurrentTrackIsNotAudioAdOnPlayQueueEvent() {
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(false);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(CURRENT_TRACK_URN));

        eventBus.verifyNoEventsOn(EventQueue.PLAYER_UI);
    }
}