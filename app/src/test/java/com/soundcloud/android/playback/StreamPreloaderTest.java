package com.soundcloud.android.playback;


import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

public class StreamPreloaderTest extends AndroidUnitTest {

    private StreamPreloader preloader;

    @Mock private TrackRepository trackRepository;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private OfflinePlaybackOperations offlinePlaybackOperations;
    @Mock private PlaybackServiceController serviceInitiator;
    @Mock private StreamCacheConfig.SkippyConfig streamCacheConfig;

    private final TestEventBus eventBus = new TestEventBus();
    private final Urn nextTrackUrn = Urn.forTrack(123L);
    private Track track;
    private final PreloadItem preloadItem = new AutoParcel_PreloadItem(nextTrackUrn, PlaybackType.AUDIO_SNIPPET);

    @Before
    public void setUp() throws Exception {
        preloader = new StreamPreloader(eventBus, trackRepository, playQueueManager,
                                        offlinePlaybackOperations, serviceInitiator, streamCacheConfig);
        preloader.subscribe();
        track = ModelFixtures.trackBuilder().urn(nextTrackUrn).snipped(true).build();
        when(trackRepository.track(nextTrackUrn)).thenReturn(Observable.just(track));
    }

    @Test
    public void preloadsWhenConditionsMetOnWifi() {
        setupValidNextTrack();
        setupValidSpaceRemaining();

        firePlayQueueItemChanged();
        publishValidPlaybackConditions();

        verify(serviceInitiator).preload(preloadItem);
    }

    @Test
    public void preloadsWhenConditionsMetOnMobileAndWithinProgressTolerance() {
        setupValidNextTrack();
        setupValidSpaceRemaining();

        firePlayQueueItemChanged();

        final int position = 1000;
        final long duration = position + StreamPreloader.MOBILE_TIME_TOLERANCE - 1;
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS,
                         PlaybackProgressEvent.create(new PlaybackProgress(position, duration, Urn.forTrack(123)), Urn.NOT_SET));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.playing());
        eventBus.publish(EventQueue.NETWORK_CONNECTION_CHANGED, ConnectionType.TWO_G);

        verify(serviceInitiator).preload(preloadItem);
    }

    @Test
    public void doesNotPreloadWhenNextTrackIsOffline() {
        when(offlinePlaybackOperations.shouldPlayOffline(track)).thenReturn(true);
        setupValidNextTrack();
        setupValidSpaceRemaining();

        firePlayQueueItemChanged();
        publishValidPlaybackConditions();

        verify(serviceInitiator, never()).preload(any(PreloadItem.class));
    }

    @Test
    public void doesNotPreloadWhenNoNextPlayQueueItem() {
        when(playQueueManager.hasNextItem()).thenReturn(false);
        when(playQueueManager.getNextPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(nextTrackUrn));
        setupValidSpaceRemaining();

        firePlayQueueItemChanged();
        publishValidPlaybackConditions();

        verify(serviceInitiator, never()).preload(any(PreloadItem.class));
    }

    @Test
    public void doesNotPreloadWhenNextPlayQueueItemNotTrack() {
        when(playQueueManager.hasNextItem()).thenReturn(true);
        when(playQueueManager.getNextPlayQueueItem()).thenReturn(new VideoAdQueueItem(AdFixtures.getVideoAd(Urn.NOT_SET)));

        setupValidSpaceRemaining();

        firePlayQueueItemChanged();
        publishValidPlaybackConditions();

        verify(serviceInitiator, never()).preload(any(PreloadItem.class));
    }

    @Test
    public void doesNotPreloadWithoutEnoughSpaceInCache() {
        setupValidNextTrack();
        when(streamCacheConfig.getRemainingCacheSpace()).thenReturn(StreamPreloader.CACHE_CUSHION - 1);

        firePlayQueueItemChanged();
        publishValidPlaybackConditions();

        verify(serviceInitiator, never()).preload(any(PreloadItem.class));
    }


    @Test
    public void doesNotPreloadWhenNotPlaying() {
        setupValidNextTrack();
        setupValidSpaceRemaining();

        firePlayQueueItemChanged();

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.buffering());
        eventBus.publish(EventQueue.NETWORK_CONNECTION_CHANGED, ConnectionType.WIFI);
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS,
                         PlaybackProgressEvent.create(PlaybackProgress.empty(), Urn.NOT_SET));

        verify(serviceInitiator, never()).preload(any(PreloadItem.class));
    }

    @Test
    public void doesNotPreloadWhenNetworkConnectionNotAvailable() {
        setupValidNextTrack();
        setupValidSpaceRemaining();

        firePlayQueueItemChanged();
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.playing());
        eventBus.publish(EventQueue.NETWORK_CONNECTION_CHANGED, ConnectionType.UNKNOWN);
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS,
                         PlaybackProgressEvent.create(PlaybackProgress.empty(), Urn.NOT_SET));

        verify(serviceInitiator, never()).preload(any(PreloadItem.class));
    }

    @Test
    public void doesNotPreloadWhenOnMobileAndNotWithinTimeTolerance() {
        setupValidNextTrack();
        setupValidSpaceRemaining();

        firePlayQueueItemChanged();
        final int position = 1000;
        final long duration = position + StreamPreloader.MOBILE_TIME_TOLERANCE;
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS,
                         PlaybackProgressEvent.create(new PlaybackProgress(position, duration, Urn.forTrack(123)), Urn.NOT_SET));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.playing());
        eventBus.publish(EventQueue.NETWORK_CONNECTION_CHANGED, ConnectionType.TWO_G);

        verify(serviceInitiator, never()).preload(any(PreloadItem.class));
    }

    private void setupValidNextTrack() {
        when(playQueueManager.hasNextItem()).thenReturn(true);
        when(playQueueManager.getNextPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(nextTrackUrn));
    }

    private void setupValidSpaceRemaining() {
        when(streamCacheConfig.getRemainingCacheSpace()).thenReturn(StreamPreloader.CACHE_CUSHION + 1);
    }

    private void firePlayQueueItemChanged() {
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(PlayQueueItem.EMPTY, Urn.NOT_SET, 0));
    }

    private void publishValidPlaybackConditions() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.playing());
        eventBus.publish(EventQueue.NETWORK_CONNECTION_CHANGED, ConnectionType.WIFI);
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS,
                         PlaybackProgressEvent.create(PlaybackProgress.empty(), Urn.NOT_SET));
    }
}
