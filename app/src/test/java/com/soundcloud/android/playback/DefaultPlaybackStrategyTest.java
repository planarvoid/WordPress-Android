package com.soundcloud.android.playback;

import static com.soundcloud.android.testsupport.PlayQueueAssertions.assertPlayQueueSet;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
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
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
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

    private DefaultPlaybackStrategy defaultPlaybackStrategy;

    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaybackServiceController serviceInitiator;
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
        defaultPlaybackStrategy = new DefaultPlaybackStrategy(playQueueManager,
                                                              serviceInitiator,
                                                              trackRepository,
                                                              offlinePlaybackOperations,
                                                              playSessionStateProvider,
                                                              eventBus);
    }

    @Test
    public void pausePausesTrackThroughService() throws Exception {
        defaultPlaybackStrategy.pause();

        verify(serviceInitiator).pause();
    }

    @Test
    public void fadeAndPausePausesTrackThroughService() throws Exception {
        defaultPlaybackStrategy.fadeAndPause();

        verify(serviceInitiator).fadeAndPause();
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
        when(playSessionStateProvider.getLastProgressForItem(trackUrn)).thenReturn(new PlaybackProgress(123L, 456L, trackUrn));
        final TrackItem track = onlineTrack();
        when(trackRepository.trackItem(trackUrn)).thenReturn(Observable.just(track));

        defaultPlaybackStrategy.resume();

        verify(serviceInitiator).play(AudioPlaybackItem.create(track, 123L));
    }

    @Test
    public void resumePlaysCurrentVideoThroughServiceIfServiceNotStarted() throws Exception {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forDestroyed());

        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createVideo(videoAd));
        when(playSessionStateProvider.getLastProgressForItem(videoAd.getAdUrn())).thenReturn(new PlaybackProgress(123L,
                                                                                                                  456L,
                                                                                                                  videoAd.getAdUrn()));

        defaultPlaybackStrategy.resume();

        verify(serviceInitiator).play(VideoAdPlaybackItem.create(videoAd, 123L));
    }

    @Test
    public void togglePlaybackPlaysCurrentTrackIfPlaybackIntentIfServiceStarted() throws Exception {
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forDestroyed());

        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(playSessionStateProvider.getLastProgressForItem(trackUrn)).thenReturn(new PlaybackProgress(123L, 456L, trackUrn));
        final TrackItem track = onlineTrack();
        when(trackRepository.trackItem(trackUrn)).thenReturn(Observable.just(track));

        defaultPlaybackStrategy.togglePlayback();

        verify(serviceInitiator).play(AudioPlaybackItem.create(track, 123L));
    }

    @Test
    public void playCurrentPlaysNormalTrackSuccessfully() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(playSessionStateProvider.getLastProgressForItem(trackUrn)).thenReturn(new PlaybackProgress(123L, 456L, trackUrn));
        final TrackItem track = onlineTrack();
        when(trackRepository.trackItem(trackUrn)).thenReturn(Observable.just(track));

        defaultPlaybackStrategy.playCurrent().subscribe(playCurrentSubscriber);

        verify(serviceInitiator).play(AudioPlaybackItem.create(track, 123L));
        playCurrentSubscriber.assertCompleted();
    }

    @Test
    public void playCurrentPlaysOfflineTrackSuccessfully() {
        final TrackItem offlineTrack = offlineTrack();
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(playSessionStateProvider.getLastProgressForItem(trackUrn)).thenReturn(new PlaybackProgress(123L, 456L, trackUrn));
        when(offlinePlaybackOperations.shouldPlayOffline(offlineTrack)).thenReturn(true);
        when(trackRepository.trackItem(trackUrn)).thenReturn(Observable.just(offlineTrack));

        defaultPlaybackStrategy.playCurrent().subscribe(playCurrentSubscriber);

        verify(serviceInitiator).play(AudioPlaybackItem.forOffline(offlineTrack, 123L));
        playCurrentSubscriber.assertCompleted();
    }

    @Test
    public void playCurrentPlaysSnippetTrackSuccessfully() {
        final TrackItem offlineTrack = onlineSnippedTrack();
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(playSessionStateProvider.getLastProgressForItem(trackUrn)).thenReturn(new PlaybackProgress(123L, 456L, trackUrn));
        when(offlinePlaybackOperations.shouldPlayOffline(offlineTrack)).thenReturn(false);
        when(trackRepository.trackItem(trackUrn)).thenReturn(Observable.just(offlineTrack));

        defaultPlaybackStrategy.playCurrent().subscribe(playCurrentSubscriber);

        verify(serviceInitiator).play(AudioPlaybackItem.forSnippet(offlineTrack, 123L));
        playCurrentSubscriber.assertCompleted();
    }

    @Test
    public void playCurrentPlaysAudioAdSuccessfully() {
        final AudioAd audioAd = AdFixtures.getAudioAd(trackUrn);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createAudioAd(audioAd));
        when(playSessionStateProvider.getLastProgressForItem(audioAd.getAdUrn())).thenReturn(PlaybackProgress.empty());

        defaultPlaybackStrategy.playCurrent().subscribe(playCurrentSubscriber);

        verify(serviceInitiator).play(AudioAdPlaybackItem.create(audioAd));
        playCurrentSubscriber.assertCompleted();
    }

    @Test
    public void playCurrentReturnsErrorOnBlockedTrack() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);
        final TrackItem track = onlineTrack();
        track.setBlocked(true);
        when(trackRepository.trackItem(trackUrn)).thenReturn(Observable.just(track));

        defaultPlaybackStrategy.playCurrent().subscribe(playCurrentSubscriber);

        verify(serviceInitiator, never()).play(any(PlaybackItem.class));
        playCurrentSubscriber.assertError(BlockedTrackException.class);
    }

    @Test
    public void playCurrentPlaysVideoAdSuccessfully() {
        final VideoAd videoAd = AdFixtures.getVideoAd(TRACK1);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createVideo(videoAd));
        when(playSessionStateProvider.getLastProgressForItem(videoAd.getAdUrn())).thenReturn(PlaybackProgress.empty());

        defaultPlaybackStrategy.playCurrent().subscribe(playCurrentSubscriber);

        verify(serviceInitiator).play(VideoAdPlaybackItem.create(videoAd, 0));
        playCurrentSubscriber.assertCompleted();
    }

    private TrackItem onlineTrack() {
        return TestPropertySets.trackWith(PropertySet.from(
                TrackProperty.URN.bind(trackUrn),
                TrackProperty.SNIPPET_DURATION.bind(123L),
                TrackProperty.FULL_DURATION.bind(456L),
                TrackProperty.SNIPPED.bind(false),
                OfflineProperty.OFFLINE_STATE.bind(OfflineState.NOT_OFFLINE)
        ));
    }

    private TrackItem onlineSnippedTrack() {
        final TrackItem trackItem = onlineTrack();
        trackItem.setSnipped(true);
        return trackItem;
    }

    private TrackItem offlineTrack() {
        return TestPropertySets.trackWith(PropertySet.from(
                TrackProperty.URN.bind(trackUrn),
                TrackProperty.SNIPPET_DURATION.bind(123L),
                TrackProperty.FULL_DURATION.bind(456L),
                TrackProperty.SNIPPED.bind(false),
                OfflineProperty.OFFLINE_STATE.bind(OfflineState.DOWNLOADED)

        ));
    }

    @Test
    public void setNewQueueOpensReturnsPlaybackResult() {
        final PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;

        defaultPlaybackStrategy.setNewQueue(getPlayQueue(playSessionSource, asList(TRACK1)),
                                            TRACK1,
                                            0,
                                            playSessionSource).subscribe(playNewQueueSubscriber);

        assertThat(playNewQueueSubscriber.getOnNextEvents().get(0).isSuccess()).isTrue();
        playNewQueueSubscriber.assertTerminalEvent();
    }

    @Test
    public void playNewQueueShouldFallBackToPositionZeroIfInitialTrackNotFound() {
        PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;
        defaultPlaybackStrategy.setNewQueue(
                getPlayQueue(playSessionSource, asList(TRACK1, TRACK2)), TRACK1, 2, playSessionSource)
                               .subscribe(playNewQueueSubscriber);

        PlayQueue expectedPlayQueue = getPlayQueue(playSessionSource, asList(TRACK1, TRACK2));
        assertPlayQueueSet(playQueueManager, expectedPlayQueue, PlaySessionSource.EMPTY, 0);
    }

    @NonNull
    private PlayQueue getPlayQueue(PlaySessionSource playSessionSource, List<Urn> trackUrns) {
        return TestPlayQueue.fromUrns(trackUrns, playSessionSource);
    }
}
