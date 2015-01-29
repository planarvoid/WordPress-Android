package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineSyncEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import java.util.ArrayList;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class TrackLikesHeaderControllerTest {

    private TrackLikesHeaderController controller;

    @Mock private TrackLikesHeaderPresenter headerPresenter;
    @Mock private OfflineSyncEventOperations offlineContentEventsOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private PlaybackOperations playbackOperations;
    private TestEventBus eventBus = new TestEventBus();
    private ArrayList<Urn> likedTrackUrns;


    @Before
    public void setUp() throws Exception {
        controller = new TrackLikesHeaderController(headerPresenter,
                offlineContentEventsOperations,
                offlineContentOperations,
                featureOperations,
                playbackOperations,
                TestSubscribers.expandPlayerSubscriber(),
                eventBus);

        when(offlineContentEventsOperations.onStarted()).thenReturn(Observable.<OfflineSyncEvent>never());
        when(offlineContentEventsOperations.onFinishedOrIdleWithDownloadedCount()).thenReturn(Observable.<Integer>never());

        likedTrackUrns = Lists.newArrayList(Urn.forTrack(123L), Urn.forTrack(456L));
        controller.setLikedTrackUrns(likedTrackUrns);
    }

    // Reaction to Offline Content Sync events
    //
    // On Sync Started
    @Test
    public void showHeaderDefaultOnSyncStartedWithOfflineSyncAvailable() {
        when(offlineContentEventsOperations.onStarted()).thenReturn(Observable.just(OfflineSyncEvent.start()));
        when(featureOperations.isOfflineSyncEnabled()).thenReturn(true);

        controller.onResume(null);

        verify(headerPresenter).showDefaultState(likedTrackUrns.size());
    }

    @Test
    public void showHeaderDefaultOnSyncStartedWithOfflineSyncEnabled() {
        when(offlineContentEventsOperations.onStarted()).thenReturn(Observable.just(OfflineSyncEvent.start()));
        when(offlineContentOperations.isLikesOfflineSyncEnabled()).thenReturn(true);

        controller.onResume(null);

        verify(headerPresenter).showDefaultState(likedTrackUrns.size());
    }

    @Test
    public void showHeaderSyncingOnSyncStartedWithOfflineSyncEnabledAndAvailable() {
        when(offlineContentEventsOperations.onStarted()).thenReturn(Observable.just(OfflineSyncEvent.start()));
        when(featureOperations.isOfflineSyncEnabled()).thenReturn(true);
        when(offlineContentOperations.isLikesOfflineSyncEnabled()).thenReturn(true);

        controller.onResume(null);

        verify(headerPresenter).showSyncingState();
    }

    // On Sync Finished or Idle
    @Test
    public void showHeaderDefaultOnSyncFinishedOrIdleWithDownloadedTracks() {
        when(offlineContentEventsOperations.onFinishedOrIdleWithDownloadedCount()).thenReturn(Observable.just(3));
        controller.onResume(null);
        verify(headerPresenter).showDefaultState(likedTrackUrns.size());
    }

    @Test
    public void showHeaderDefaultOnSyncFinishedOrIdleWithOfflineSyncAvailable() {
        when(offlineContentEventsOperations.onFinishedOrIdleWithDownloadedCount()).thenReturn(Observable.just(3));
        when(featureOperations.isOfflineSyncEnabled()).thenReturn(true);

        controller.onResume(null);

        verify(headerPresenter).showDefaultState(likedTrackUrns.size());
    }

    @Test
    public void showHeaderDefaultOnSyncFinishedOrIdleWithOfflineSyncEnabled() {
        when(offlineContentEventsOperations.onFinishedOrIdleWithDownloadedCount()).thenReturn(Observable.just(3));
        when(offlineContentOperations.isLikesOfflineSyncEnabled()).thenReturn(true);

        controller.onResume(null);

        verify(headerPresenter).showDefaultState(likedTrackUrns.size());
    }

    @Test
    public void showHeaderDownloadedOnSyncFinishedOrIdleWithDownloadTracksAndOfflineAvailableAndEnabled() {
        when(offlineContentEventsOperations.onFinishedOrIdleWithDownloadedCount()).thenReturn(Observable.just(3));
        when(featureOperations.isOfflineSyncEnabled()).thenReturn(true);
        when(offlineContentOperations.isLikesOfflineSyncEnabled()).thenReturn(true);

        controller.onResume(null);

        verify(headerPresenter).showDownloadedState(likedTrackUrns.size());
    }


    // Shuffle button click
    @Test
    public void emitTrackingEventOnShuffleButtonClick() {
        when(playbackOperations.playTracksShuffled(eq(likedTrackUrns), any(PlaySessionSource.class)))
                .thenReturn(Observable.<List<Urn>>empty());

        controller.onClick(null);

        expect(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).toEqual(UIEvent.KIND_SHUFFLE_LIKES);
    }

}