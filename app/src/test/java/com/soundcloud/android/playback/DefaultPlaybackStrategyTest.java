package com.soundcloud.android.playback;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ServiceInitiator;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.observers.TestSubscriber;

public class DefaultPlaybackStrategyTest extends AndroidUnitTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final Urn TRACK2 = Urn.forTrack(456L);
    private static final Urn TRACK3 = Urn.forTrack(789L);

    private DefaultPlaybackStrategy defaultPlaybackStrategy;


    @Mock private PlayQueueManager playQueueManager;
    @Mock private ServiceInitiator serviceInitiator;
    @Mock private TrackRepository trackRepository;
    @Mock private AdsOperations adsOperations;
    @Mock private OfflinePlaybackOperations offlinePlaybackOperations;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    private TestEventBus eventBus = new TestEventBus();

    private final Urn trackUrn = Urn.forTrack(123L);

    private TestSubscriber<Void> playCurrentSubscriber = new TestSubscriber<>();
    private TestObserver<PlaybackResult> playNewQueueSubscriber = new TestObserver<>();

    @Before
    public void setUp() throws Exception {
        defaultPlaybackStrategy = new DefaultPlaybackStrategy(playQueueManager, serviceInitiator,
                trackRepository, adsOperations, offlinePlaybackOperations, playSessionStateProvider, eventBus);
    }

    @Test
    public void pausePausesTrackThroughService() throws Exception {
        defaultPlaybackStrategy.pause();

        verify(serviceInitiator).pause();
    }

    @Test
    public void resumePlaysTrackThroughServiceIfServiceStarted() throws Exception {
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());

        defaultPlaybackStrategy.resume();

        verify(serviceInitiator).resume();
    }

    @Test
    public void togglePlaybackSendsTogglePlaybackIntentIfServiceStarted() throws Exception {
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());

        defaultPlaybackStrategy.togglePlayback();

        verify(serviceInitiator).togglePlayback();
    }

    @Test
    public void resumePlaysCurrentTrackThroughServiceIfServiceNotStarted() throws Exception {
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forDestroyed());

        when(playQueueManager.getCurrentTrackUrn()).thenReturn(trackUrn);
        when(playSessionStateProvider.getLastProgressForTrack(trackUrn)).thenReturn(new PlaybackProgress(123L, 456L));
        when(trackRepository.track(trackUrn)).thenReturn(Observable.just(onlineTrack()));

        defaultPlaybackStrategy.resume();

        verify(serviceInitiator).play(trackUrn, 123L);
    }

    @Test
    public void togglePlaybackPlaysCurrentTrackIfPlaybackIntentIfServiceStarted() throws Exception {
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forDestroyed());

        when(playQueueManager.getCurrentTrackUrn()).thenReturn(trackUrn);
        when(playSessionStateProvider.getLastProgressForTrack(trackUrn)).thenReturn(new PlaybackProgress(123L, 456L));
        when(trackRepository.track(trackUrn)).thenReturn(Observable.just(onlineTrack()));

        defaultPlaybackStrategy.togglePlayback();

        verify(serviceInitiator).play(trackUrn, 123L);
    }

    @Test
    public void playCurrentPlaysNormalTrackSuccessfully() {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(trackUrn);
        when(playSessionStateProvider.getLastProgressForTrack(trackUrn)).thenReturn(new PlaybackProgress(123L, 456L));
        when(trackRepository.track(trackUrn)).thenReturn(Observable.just(onlineTrack()));

        defaultPlaybackStrategy.playCurrent().subscribe(playCurrentSubscriber);

        verify(serviceInitiator).play(trackUrn, 123L);
        playCurrentSubscriber.assertValueCount(1);
    }

    @Test
    public void playCurrentPlaysOfflineTrackSuccessfully() {
        final PropertySet offlineTrack = offlineTrack();
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(trackUrn);
        when(playSessionStateProvider.getLastProgressForTrack(trackUrn)).thenReturn(new PlaybackProgress(123L, 456L));
        when(offlinePlaybackOperations.shouldPlayOffline(offlineTrack)).thenReturn(true);
        when(trackRepository.track(trackUrn)).thenReturn(Observable.just(offlineTrack));

        defaultPlaybackStrategy.playCurrent().subscribe(playCurrentSubscriber);

        verify(serviceInitiator).playOffline(trackUrn, 123L);
        playCurrentSubscriber.assertValueCount(1);
    }

    @Test
    public void playCurrentPlaysAdSuccessfully() {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(trackUrn);
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        when(trackRepository.track(trackUrn)).thenReturn(Observable.just(onlineTrack()));

        defaultPlaybackStrategy.playCurrent().subscribe(playCurrentSubscriber);

        verify(serviceInitiator).playUninterrupted(trackUrn);
        playCurrentSubscriber.assertValueCount(1);
    }

    private PropertySet onlineTrack() {
        return PropertySet.from(
                TrackProperty.URN.bind(trackUrn),
                OfflineProperty.OFFLINE_STATE.bind(OfflineState.NO_OFFLINE)

        );
    }

    private PropertySet offlineTrack() {
        return PropertySet.from(
                TrackProperty.URN.bind(trackUrn),
                OfflineProperty.OFFLINE_STATE.bind(OfflineState.DOWNLOADED)

        );
    }

    @Test
    public void setNewQueueOpensReturnsPlaybackResult() {
        final PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;

        defaultPlaybackStrategy.setNewQueue(PlayQueue.fromTrackUrnList(asList(TRACK1), playSessionSource), TRACK1, 0, false, playSessionSource).subscribe(playNewQueueSubscriber);

        assertThat(playNewQueueSubscriber.getOnNextEvents().get(0).isSuccess()).isTrue();
        playNewQueueSubscriber.assertTerminalEvent();
    }

    @Test
    public void playNewQueueRemovesDuplicates() {
        PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;
        defaultPlaybackStrategy.setNewQueue(
                PlayQueue.fromTrackUrnList(asList(TRACK1, TRACK2, TRACK3, TRACK2, TRACK1), playSessionSource), TRACK1, 0, false, playSessionSource).subscribe(playNewQueueSubscriber);

        PlayQueue expectedPlayQueue = PlayQueue.fromTrackUrnList(asList(TRACK1, TRACK2, TRACK3), playSessionSource);
        verify(playQueueManager).setNewPlayQueue(eq(expectedPlayQueue), eq(playSessionSource), eq(0));
    }

    @Test
    public void playNewQueueShouldFallBackToPositionZeroIfInitialTrackNotFound() {
        PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;
        defaultPlaybackStrategy.setNewQueue(
                PlayQueue.fromTrackUrnList(asList(TRACK1, TRACK2), playSessionSource), TRACK1, 2, false, playSessionSource).subscribe(playNewQueueSubscriber);

        PlayQueue expectedPlayQueue = PlayQueue.fromTrackUrnList(asList(TRACK1, TRACK2), playSessionSource);
        verify(playQueueManager).setNewPlayQueue(eq(expectedPlayQueue), eq(playSessionSource), eq(0));
    }
}
