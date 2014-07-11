package com.soundcloud.android.ads;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.TrackProperty;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.track.TrackOperations;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;

import java.io.IOException;

@RunWith(SoundCloudTestRunner.class)
public class AdsControllerTest {

    private static final TrackUrn CURRENT_TRACK_URN = Urn.forTrack(122L);
    private static final TrackUrn NEXT_TRACK_URN = Urn.forTrack(123L);
    private static final PropertySet MONETIZEABLE_PROPERTY_SET = PropertySet.from(TrackProperty.URN.bind(NEXT_TRACK_URN), TrackProperty.MONETIZABLE.bind(true));
    private static final PropertySet NON_MONETIZEABLE_PROPERTY_SET = PropertySet.from(TrackProperty.URN.bind(NEXT_TRACK_URN), TrackProperty.MONETIZABLE.bind(false));

    @Mock private PlayQueueManager playQueueManager;
    @Mock private AdsOperations adsOperations;
    @Mock private TrackOperations trackOperations;
    @Mock private AccountOperations accountOperations;

    private TestEventBus eventBus = new TestEventBus();
    private AudioAd audioAd;

    @Before
    public void setUp() throws Exception {
        AdsController adsController = new AdsController(eventBus, adsOperations, playQueueManager, trackOperations);
        adsController.subscribe();
        audioAd = TestHelper.getModelFactory().createModel(AudioAd.class);
    }

    @Test
    public void trackChangeEventInsertsAudioAdIntoPlayQueue() throws CreateModelException {
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(Observable.just(MONETIZEABLE_PROPERTY_SET));
        when(adsOperations.audioAd(NEXT_TRACK_URN)).thenReturn(Observable.just(audioAd));

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(CURRENT_TRACK_URN));

        verify(playQueueManager).insertAd(audioAd);
    }

    @Test
    public void trackChangeEventDoesNothingIfNoNextTrack() {
        when(playQueueManager.hasNextTrack()).thenReturn(false);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(CURRENT_TRACK_URN));

        verify(playQueueManager, never()).insertAd(any(AudioAd.class));
    }

    @Test
    public void trackChangeEventDoesNothingIfNextTrackIsAudioAd() {
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(Observable.just(MONETIZEABLE_PROPERTY_SET));
        when(adsOperations.audioAd(NEXT_TRACK_URN)).thenReturn(Observable.just(audioAd));
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(playQueueManager.isNextTrackAudioAd()).thenReturn(true);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate(CURRENT_TRACK_URN));

        verify(playQueueManager, never()).insertAd(any(AudioAd.class));
    }

    @Test
    public void trackChangeEventDoesNothingIfAlreadyPlayingAd() {
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(Observable.just(MONETIZEABLE_PROPERTY_SET));
        when(adsOperations.audioAd(NEXT_TRACK_URN)).thenReturn(Observable.just(audioAd));
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate(CURRENT_TRACK_URN));

        verify(playQueueManager, never()).insertAd(any(AudioAd.class));
    }

    @Test
    public void trackChangeEventDoesNothingIfNextTrackIsNotMonetizeable() {
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(Observable.just(NON_MONETIZEABLE_PROPERTY_SET));

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(CURRENT_TRACK_URN));

        verify(playQueueManager, never()).insertAd(any(AudioAd.class));
    }

    @Test
    public void trackChangeEventDoesNothingIfAudioAdFetchEmitsError() {
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(Observable.just(MONETIZEABLE_PROPERTY_SET));
        when(adsOperations.audioAd(NEXT_TRACK_URN)).thenReturn(Observable.<AudioAd>error(new IOException("Ad fetch error")));

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(CURRENT_TRACK_URN));

        verify(playQueueManager, never()).insertAd(any(AudioAd.class));
    }

    @Test
    public void unsubscribesFromCurrentAdFetchOnPlayQueueEvent() throws Exception {
        Subscription adFetchSubscription = mock(Subscription.class);

        when(playQueueManager.hasNextTrack()).thenReturn(true);
        when(playQueueManager.getNextTrackUrn()).thenReturn(NEXT_TRACK_URN);
        when(trackOperations.track(NEXT_TRACK_URN)).thenReturn(Observable.just(MONETIZEABLE_PROPERTY_SET));
        when(adsOperations.audioAd(NEXT_TRACK_URN)).thenReturn(TestObservables.<AudioAd>endlessObservablefromSubscription(adFetchSubscription));

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(CURRENT_TRACK_URN));
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(CURRENT_TRACK_URN));

        verify(adFetchSubscription).unsubscribe();
    }
}