package com.soundcloud.android.likes;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineContentEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.presentation.ListBinding;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.support.v4.app.Fragment;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class TrackLikesHeaderPresenterTest {

    private TrackLikesHeaderPresenter presenter;

    @Mock private TrackLikesHeaderView headerView;
    @Mock private LikeOperations likeOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private ListBinding<PropertySet, PropertySet> listBinding;
    @Mock private Fragment fragment;
    private TestEventBus eventBus = new TestEventBus();
    private List<Urn> likedTrackUrns;

    @Before
    public void setUp() throws Exception {
        presenter = new TrackLikesHeaderPresenter(headerView,
                likeOperations,
                offlineContentOperations,
                featureOperations,
                playbackOperations,
                TestSubscribers.expandPlayerSubscriber(),
                eventBus);

        when(offlineContentOperations.onStarted()).thenReturn(Observable.<OfflineContentEvent>never());
        when(offlineContentOperations.onFinishedOrIdleWithDownloadedCount()).thenReturn(Observable.<Integer>never());

        likedTrackUrns = Lists.newArrayList(Urn.forTrack(123L), Urn.forTrack(456L));
    }

    // Reaction to Offline Content Sync events
    //
    // On Sync Started
    @Test
    public void showHeaderDefaultOnSyncStartedWithOfflineSyncAvailable() {
        when(offlineContentOperations.onStarted()).thenReturn(Observable.just(OfflineContentEvent.start()));
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        presenter.onResume(fragment);

        verify(headerView).showDefaultState();
    }

    @Test
    public void showHeaderDefaultOnSyncStartedWithOfflineSyncEnabled() {
        when(offlineContentOperations.onStarted()).thenReturn(Observable.just(OfflineContentEvent.start()));
        when(offlineContentOperations.isOfflineLikesEnabled()).thenReturn(true);

        presenter.onResume(fragment);

        verify(headerView).showDefaultState();
    }

    @Test
    public void showHeaderSyncingOnSyncStartedWithOfflineSyncEnabledAndAvailable() {
        when(offlineContentOperations.onStarted()).thenReturn(Observable.just(OfflineContentEvent.start()));
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(offlineContentOperations.isOfflineLikesEnabled()).thenReturn(true);

        presenter.onResume(fragment);

        verify(headerView).showSyncingState();
    }

    @Test
    public void doNotChangeHeaderOnSyncStartedEventsAfterOnPause() {
        PublishSubject<OfflineContentEvent> offlineSyncEvents = PublishSubject.create();
        when(offlineContentOperations.onStarted()).thenReturn(offlineSyncEvents);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        presenter.onResume(fragment);
        presenter.onPause(fragment);
        offlineSyncEvents.onNext(OfflineContentEvent.start());

        verifyNoMoreInteractions(headerView);
    }

    @Test
    public void doNotChangeHeaderOnSyncStoppedEventsAfterOnPause() {
        PublishSubject<OfflineContentEvent> offlineSyncEvents = PublishSubject.create();
        when(offlineContentOperations.onStarted()).thenReturn(offlineSyncEvents);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        presenter.onResume(fragment);
        presenter.onPause(fragment);
        offlineSyncEvents.onNext(OfflineContentEvent.stop());

        verifyNoMoreInteractions(headerView);
    }

    // On Sync Finished or Idle
    @Test
    public void showHeaderDefaultOnSyncFinishedOrIdleWithDownloadedTracks() {
        when(offlineContentOperations.onFinishedOrIdleWithDownloadedCount()).thenReturn(Observable.just(3));
        presenter.onResume(fragment);
        verify(headerView).showDefaultState();
    }

    @Test
    public void showHeaderDefaultOnSyncFinishedOrIdleWithOfflineSyncAvailable() {
        when(offlineContentOperations.onFinishedOrIdleWithDownloadedCount()).thenReturn(Observable.just(3));
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        presenter.onResume(fragment);

        verify(headerView).showDefaultState();
    }

    @Test
    public void showHeaderDefaultOnSyncFinishedOrIdleWithOfflineSyncEnabled() {
        when(offlineContentOperations.onFinishedOrIdleWithDownloadedCount()).thenReturn(Observable.just(3));
        when(offlineContentOperations.isOfflineLikesEnabled()).thenReturn(true);

        presenter.onResume(fragment);

        verify(headerView).showDefaultState();
    }

    @Test
    public void showHeaderDownloadedOnSyncFinishedOrIdleWithDownloadTracksAndOfflineAvailableAndEnabled() {
        when(offlineContentOperations.onFinishedOrIdleWithDownloadedCount()).thenReturn(Observable.just(3));
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(offlineContentOperations.isOfflineLikesEnabled()).thenReturn(true);

        presenter.onResume(fragment);

        verify(headerView).showDownloadedState();
    }

    @Test
    public void onSubscribeListObserversUpdatesHeaderViewTrackCountOnlyOnce() {
        when(listBinding.getSource()).thenReturn(Observable.just(TestPropertySets.expectedLikedTrackForLikesScreen()).toList());
        when(likeOperations.likedTrackUrns()).thenReturn(Observable.just(likedTrackUrns));

        presenter.onSubscribeListObservers(listBinding);

        verify(headerView).updateTrackCount(likedTrackUrns.size());
    }

    @Test
    public void doNotUpdateTrackCountAfterViewIsDetroyed() {
        when(listBinding.getSource()).thenReturn(Observable.just(TestPropertySets.expectedLikedTrackForLikesScreen()).toList());
        PublishSubject<List<Urn>> likedTrackUrnsObservable = PublishSubject.create();
        when(likeOperations.likedTrackUrns()).thenReturn(likedTrackUrnsObservable);

        presenter.onSubscribeListObservers(listBinding);
        presenter.onDestroyView();
        likedTrackUrnsObservable.onNext(likedTrackUrns);

        verify(headerView, never()).updateTrackCount(likedTrackUrns.size());
    }

    // Shuffle button click
    @Test
    public void emitTrackingEventOnShuffleButtonClick() {
        when(playbackOperations.playTracksShuffled(any(Observable.class), any(PlaySessionSource.class)))
                .thenReturn(Observable.<List<Urn>>empty());
        presenter.onClick(null);
        expect(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).toEqual(UIEvent.KIND_SHUFFLE_LIKES);
    }

}