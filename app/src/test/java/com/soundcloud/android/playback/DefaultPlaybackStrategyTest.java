package com.soundcloud.android.playback;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ServiceInitiator;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueue;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
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

import android.support.annotation.NonNull;

import java.util.List;

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
    private final PlayQueueItem trackPlayQueueItem = TestPlayQueueItem.createTrack(trackUrn);

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

        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(playSessionStateProvider.getLastProgressForTrack(trackUrn)).thenReturn(new PlaybackProgress(123L, 456L));
        final PropertySet track = onlineTrack();
        when(trackRepository.track(trackUrn)).thenReturn(Observable.just(track));

        defaultPlaybackStrategy.resume();

        verify(serviceInitiator).play(AudioPlaybackItem.create(track, 123L));
    }

    @Test
    public void togglePlaybackPlaysCurrentTrackIfPlaybackIntentIfServiceStarted() throws Exception {
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forDestroyed());

        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(playSessionStateProvider.getLastProgressForTrack(trackUrn)).thenReturn(new PlaybackProgress(123L, 456L));
        final PropertySet track = onlineTrack();
        when(trackRepository.track(trackUrn)).thenReturn(Observable.just(track));

        defaultPlaybackStrategy.togglePlayback();

        verify(serviceInitiator).play(AudioPlaybackItem.create(track, 123L));
    }

    @Test
    public void playCurrentPlaysNormalTrackSuccessfully() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(playSessionStateProvider.getLastProgressForTrack(trackUrn)).thenReturn(new PlaybackProgress(123L, 456L));
        final PropertySet track = onlineTrack();
        when(trackRepository.track(trackUrn)).thenReturn(Observable.just(track));

        defaultPlaybackStrategy.playCurrent().subscribe(playCurrentSubscriber);

        verify(serviceInitiator).play(AudioPlaybackItem.create(track, 123L));
        playCurrentSubscriber.assertCompleted();
    }

    @Test
    public void playCurrentPlaysOfflineTrackSuccessfully() {
        final PropertySet offlineTrack = offlineTrack();
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(playSessionStateProvider.getLastProgressForTrack(trackUrn)).thenReturn(new PlaybackProgress(123L, 456L));
        when(offlinePlaybackOperations.shouldPlayOffline(offlineTrack)).thenReturn(true);
        when(trackRepository.track(trackUrn)).thenReturn(Observable.just(offlineTrack));

        defaultPlaybackStrategy.playCurrent().subscribe(playCurrentSubscriber);

        verify(serviceInitiator).play(AudioPlaybackItem.forOffline(offlineTrack, 123L));
        playCurrentSubscriber.assertCompleted();
    }

    @Test
    public void playCurrentPlaysAudioAdSuccessfully() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);
        final PropertySet track = onlineTrack();
        when(trackRepository.track(trackUrn)).thenReturn(Observable.just(track));

        defaultPlaybackStrategy.playCurrent().subscribe(playCurrentSubscriber);

        verify(serviceInitiator).play(AudioPlaybackItem.forAudioAd(track));
        playCurrentSubscriber.assertCompleted();
    }

    @Test
    public void playCurrentReturnsErrorOnBlockedTrack() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);
        final PropertySet track = onlineTrack();
        track.put(TrackProperty.BLOCKED, true);
        when(trackRepository.track(trackUrn)).thenReturn(Observable.just(track));

        defaultPlaybackStrategy.playCurrent().subscribe(playCurrentSubscriber);

        verify(serviceInitiator, never()).play(any(PlaybackItem.class));
        playCurrentSubscriber.assertError(BlockedTrackException.class);
    }

    @Test
    public void playCurrentPlaysVideoAdSuccessfully() {
        final VideoAd videoAdData = AdFixtures.getVideoAd(TRACK1);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createVideo(videoAdData));

        defaultPlaybackStrategy.playCurrent().subscribe(playCurrentSubscriber);

        verify(serviceInitiator).play(VideoPlaybackItem.create(videoAdData));
        playCurrentSubscriber.assertValueCount(1);
    }

    private PropertySet onlineTrack() {
        return PropertySet.from(
                TrackProperty.URN.bind(trackUrn),
                TrackProperty.PLAY_DURATION.bind(123L),
                OfflineProperty.OFFLINE_STATE.bind(OfflineState.NO_OFFLINE)
        );
    }

    private PropertySet offlineTrack() {
        return PropertySet.from(
                TrackProperty.URN.bind(trackUrn),
                TrackProperty.PLAY_DURATION.bind(123L),
                OfflineProperty.OFFLINE_STATE.bind(OfflineState.DOWNLOADED)

        );
    }

    @Test
    public void setNewQueueOpensReturnsPlaybackResult() {
        final PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;

        defaultPlaybackStrategy.setNewQueue(getPlayQueue(playSessionSource, asList(TRACK1)), TRACK1, 0, playSessionSource).subscribe(playNewQueueSubscriber);

        assertThat(playNewQueueSubscriber.getOnNextEvents().get(0).isSuccess()).isTrue();
        playNewQueueSubscriber.assertTerminalEvent();
    }

    @Test
    public void playNewQueueRemovesDuplicates() {
        PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;
        defaultPlaybackStrategy.setNewQueue(
                getPlayQueue(playSessionSource, asList(TRACK1, TRACK2, TRACK3, TRACK2, TRACK1)), TRACK1, 0, playSessionSource).subscribe(playNewQueueSubscriber);

        PlayQueue expectedPlayQueue = getPlayQueue(playSessionSource, asList(TRACK1, TRACK2, TRACK3));
        verify(playQueueManager).setNewPlayQueue(eq(expectedPlayQueue), eq(playSessionSource), eq(0));
    }

    @Test
    public void playNewQueueShouldFallBackToPositionZeroIfInitialTrackNotFound() {
        PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;
        defaultPlaybackStrategy.setNewQueue(
                getPlayQueue(playSessionSource, asList(TRACK1, TRACK2)), TRACK1, 2, playSessionSource).subscribe(playNewQueueSubscriber);

        PlayQueue expectedPlayQueue = getPlayQueue(playSessionSource, asList(TRACK1, TRACK2));
        verify(playQueueManager).setNewPlayQueue(eq(expectedPlayQueue), eq(playSessionSource), eq(0));
    }

    @NonNull
    private PlayQueue getPlayQueue(PlaySessionSource playSessionSource, List<Urn> trackUrns) {
        return TestPlayQueue.fromUrns(trackUrns, playSessionSource);
    }
}
