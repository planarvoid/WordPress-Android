package com.soundcloud.android.playback;

import static com.soundcloud.android.testsupport.PlayQueueAssertions.assertPlayQueueSet;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueue;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import io.reactivex.Maybe;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.List;

public class DefaultPlaybackStrategyTest extends AndroidUnitTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final Urn TRACK2 = Urn.forTrack(456L);

    private DefaultPlaybackStrategy defaultPlaybackStrategy;

    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaybackServiceController serviceInitiator;
    @Mock private TrackItemRepository trackItemRepository;
    @Mock private AdsOperations adsOperations;
    @Mock private OfflinePlaybackOperations offlinePlaybackOperations;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private OfflineSettingsStorage offlineSettingsStorage;
    @Mock private FeedbackController feedbackController;
    private TestEventBusV2 eventBus = new TestEventBusV2();

    private final Urn trackUrn = Urn.forTrack(123L);
    private final PlayQueueItem trackPlayQueueItem = TestPlayQueueItem.createTrack(trackUrn);

    @Before
    public void setUp() throws Exception {
        defaultPlaybackStrategy = new DefaultPlaybackStrategy(playQueueManager,
                                                              serviceInitiator,
                                                              trackItemRepository,
                                                              offlinePlaybackOperations,
                                                              playSessionStateProvider,
                                                              eventBus,
                                                              offlineSettingsStorage,
                                                              feedbackController);
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
        final TrackItem trackItem = onlineTrack();
        when(trackItemRepository.track(trackUrn)).thenReturn(Maybe.just(trackItem));

        defaultPlaybackStrategy.resume();

        verify(serviceInitiator).play(AudioPlaybackItem.create(trackItem.track(), 123L));
    }

    @Test
    public void resumePlaysCurrentVideoThroughServiceIfServiceNotStarted() throws Exception {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forDestroyed());

        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createVideo(videoAd));
        when(playSessionStateProvider.getLastProgressForItem(videoAd.adUrn())).thenReturn(new PlaybackProgress(123L,
                                                                                                               456L,
                                                                                                               videoAd.adUrn()));

        defaultPlaybackStrategy.resume();

        verify(serviceInitiator).play(VideoAdPlaybackItem.create(videoAd, 123L));
    }

    @Test
    public void togglePlaybackPlaysCurrentTrackIfPlaybackIntentIfServiceStarted() throws Exception {
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forDestroyed());

        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(playSessionStateProvider.getLastProgressForItem(trackUrn)).thenReturn(new PlaybackProgress(123L, 456L, trackUrn));
        final TrackItem trackItem = onlineTrack();
        when(trackItemRepository.track(trackUrn)).thenReturn(Maybe.just(trackItem));

        defaultPlaybackStrategy.togglePlayback();

        verify(serviceInitiator).play(AudioPlaybackItem.create(trackItem.track(), 123L));
    }

    @Test
    public void playCurrentPlaysNormalTrackSuccessfully() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(playSessionStateProvider.getLastProgressForItem(trackUrn)).thenReturn(new PlaybackProgress(123L, 456L, trackUrn));
        final TrackItem trackItem = onlineTrack();
        when(trackItemRepository.track(trackUrn)).thenReturn(Maybe.just(trackItem));

        defaultPlaybackStrategy.playCurrent().test().assertComplete();

        verify(serviceInitiator).play(AudioPlaybackItem.create(trackItem.track(), 123L));
    }

    @Test
    public void playCurrentPlaysOfflineTrackSuccessfully() {
        final TrackItem offlineTrack = offlineTrack();
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(playSessionStateProvider.getLastProgressForItem(trackUrn)).thenReturn(new PlaybackProgress(123L, 456L, trackUrn));
        when(offlinePlaybackOperations.shouldPlayOffline(offlineTrack)).thenReturn(true);
        when(trackItemRepository.track(trackUrn)).thenReturn(Maybe.just(offlineTrack));
        when(offlineSettingsStorage.isOfflineContentAccessible()).thenReturn(true);

        defaultPlaybackStrategy.playCurrent().test().assertComplete();

        verify(serviceInitiator).play(AudioPlaybackItem.forOffline(offlineTrack.track(), 123L));
    }

    @Test
    public void playCurrentFallsBackToNormalTrackWhenOfflineContentIsNotAccessible() {
        final TrackItem offlineTrack = offlineTrack();
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(playSessionStateProvider.getLastProgressForItem(trackUrn)).thenReturn(new PlaybackProgress(123L, 456L, trackUrn));
        when(offlinePlaybackOperations.shouldPlayOffline(offlineTrack)).thenReturn(true);
        when(trackItemRepository.track(trackUrn)).thenReturn(Maybe.just(offlineTrack));
        when(offlineSettingsStorage.isOfflineContentAccessible()).thenReturn(false);

        defaultPlaybackStrategy.playCurrent().test().assertComplete();

        verify(feedbackController).showFeedback(Feedback.create(R.string.sd_card_cannot_be_found));
        verify(serviceInitiator).play(AudioPlaybackItem.create(offlineTrack.track(), 123L));
    }

    @Test
    public void playCurrentPlaysSnippetTrackSuccessfully() {
        final TrackItem offlineTrack = onlineSnippedTrack();
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(playSessionStateProvider.getLastProgressForItem(trackUrn)).thenReturn(new PlaybackProgress(123L, 456L, trackUrn));
        when(offlinePlaybackOperations.shouldPlayOffline(offlineTrack)).thenReturn(false);
        when(trackItemRepository.track(trackUrn)).thenReturn(Maybe.just(offlineTrack));

        defaultPlaybackStrategy.playCurrent().test().assertComplete();

        verify(serviceInitiator).play(AudioPlaybackItem.forSnippet(offlineTrack.track(), 123L));
    }

    @Test
    public void playCurrentPlaysAudioAdSuccessfully() {
        final AudioAd audioAd = AdFixtures.getAudioAd(trackUrn);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createAudioAd(audioAd));
        when(playSessionStateProvider.getLastProgressForItem(audioAd.adUrn())).thenReturn(PlaybackProgress.empty());

        defaultPlaybackStrategy.playCurrent().test().assertComplete();

        verify(serviceInitiator).play(AudioAdPlaybackItem.create(audioAd));
    }

    @Test
    public void playCurrentReturnsErrorOnBlockedTrack() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);
        final Track.Builder builder = onlineTrackBuilder();
        builder.blocked(true);
        TrackItem trackItem = ModelFixtures.trackItem(builder.build());
        when(trackItemRepository.track(trackUrn)).thenReturn(Maybe.just(trackItem));

        TestObserver<Void> testObserver = defaultPlaybackStrategy.playCurrent().test();

        verify(serviceInitiator, never()).play(any(PlaybackItem.class));
        testObserver.assertError(BlockedTrackException.class);
    }

    @Test
    public void playCurrentPlaysVideoAdSuccessfully() {
        final VideoAd videoAd = AdFixtures.getVideoAd(TRACK1);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createVideo(videoAd));
        when(playSessionStateProvider.getLastProgressForItem(videoAd.adUrn())).thenReturn(PlaybackProgress.empty());

        defaultPlaybackStrategy.playCurrent().test().assertComplete();

        verify(serviceInitiator).play(VideoAdPlaybackItem.create(videoAd, 0));
    }

    private TrackItem onlineTrack() {
        return ModelFixtures.trackItem(onlineTrackBuilder().build());
    }

    private Track.Builder onlineTrackBuilder() {
        return ModelFixtures.trackBuilder().urn(trackUrn).snippetDuration(123L).fullDuration(456L).snipped(false).offlineState(OfflineState.NOT_OFFLINE);
    }

    private TrackItem onlineSnippedTrack() {
        final Track.Builder builder = onlineTrackBuilder();
        builder.snipped(true);
        return ModelFixtures.trackItem(builder.build());
    }

    private TrackItem offlineTrack() {
        return ModelFixtures.trackItem(ModelFixtures.trackBuilder().urn(trackUrn).snippetDuration(123L).fullDuration(456L).snipped(false).offlineState(OfflineState.DOWNLOADED).build());
    }

    @Test
    public void setNewQueueOpensReturnsPlaybackResult() {
        final PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;

        TestObserver<PlaybackResult> testObserver = defaultPlaybackStrategy.setNewQueue(getPlayQueue(playSessionSource, Collections.singletonList(TRACK1)),
                                                                                        TRACK1,
                                                                                        0,
                                                                                        playSessionSource)
                                                                           .test();

        assertThat(testObserver.values().get(0).isSuccess()).isTrue();
        testObserver.assertTerminated();
    }

    @Test
    public void playNewQueueShouldFallBackToPositionZeroIfInitialTrackNotFound() {
        PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;
        defaultPlaybackStrategy.setNewQueue(
                getPlayQueue(playSessionSource, asList(TRACK1, TRACK2)), TRACK1, 2, playSessionSource)
                               .test();

        PlayQueue expectedPlayQueue = getPlayQueue(playSessionSource, asList(TRACK1, TRACK2));
        assertPlayQueueSet(playQueueManager, expectedPlayQueue, PlaySessionSource.EMPTY, 0);
    }

    @NonNull
    private PlayQueue getPlayQueue(PlaySessionSource playSessionSource, List<Urn> trackUrns) {
        return TestPlayQueue.fromUrns(trackUrns, playSessionSource);
    }
}
