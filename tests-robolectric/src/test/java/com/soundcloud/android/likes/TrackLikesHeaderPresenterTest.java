package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadRequest;
import com.soundcloud.android.offline.DownloadState;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.presentation.ListBinding;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ListView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class TrackLikesHeaderPresenterTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final DownloadRequest TRACK1_DOWNLOAD_REQUEST = new DownloadRequest(TRACK1, "http://track1", 0, true, Collections.<Urn>emptyList());
    private static final Urn TRACK2 = Urn.forTrack(456L);
    private TrackLikesHeaderPresenter presenter;

    @Mock private TrackLikesHeaderView headerView;
    @Mock private TrackLikeOperations likeOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private OfflinePlaybackOperations playbackOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private ListBinding<PropertySet, TrackItem> listBinding;
    @Mock private Fragment fragment;
    @Mock private View layoutView;
    @Mock private ListView listView;
    private TestEventBus eventBus;
    private List<Urn> likedTrackUrns;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        presenter = new TrackLikesHeaderPresenter(
                headerView,
                likeOperations,
                offlineContentOperations,
                playbackOperations,
                TestSubscribers.expandPlayerSubscriber(),
                featureOperations,
                eventBus);

        likedTrackUrns = Lists.newArrayList(TRACK1, TRACK2);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(offlineContentOperations.getLikedTracksDownloadStateFromStorage()).thenReturn(Observable.<DownloadState>never());
    }

    @Test
    public void onSubscribeListObserversUpdatesHeaderViewTrackCountOnlyOnce() {
        when(listBinding.getSource()).thenReturn(
                Observable.just(TrackItem.from(TestPropertySets.expectedLikedTrackForLikesScreen())).toList());
        when(likeOperations.likedTrackUrns()).thenReturn(Observable.just(likedTrackUrns));

        presenter.onViewCreated(layoutView, listView);
        presenter.onSubscribeListObservers(listBinding);

        verify(headerView).updateTrackCount(likedTrackUrns.size());
    }

    @Test
    public void doNotUpdateTrackCountAfterViewIsDestroyed() {
        when(listBinding.getSource()).thenReturn(
                Observable.just(TrackItem.from(TestPropertySets.expectedLikedTrackForLikesScreen())).toList());
        PublishSubject<List<Urn>> likedTrackUrnsObservable = PublishSubject.create();
        when(likeOperations.likedTrackUrns()).thenReturn(likedTrackUrnsObservable);

        presenter.onViewCreated(layoutView, listView);
        presenter.onSubscribeListObservers(listBinding);
        presenter.onDestroyView(fragment);
        likedTrackUrnsObservable.onNext(likedTrackUrns);

        verify(headerView, never()).updateTrackCount(anyInt());
    }

    @Test
    public void updateTrackCountOnEntityLikeEvent() {
        when(likeOperations.likedTrackUrns()).thenReturn(Observable.just(likedTrackUrns));
        presenter.onViewCreated(layoutView, listView);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(TRACK1, true, 5));

        verify(headerView).updateTrackCount(2);
    }

    @Test
    public void doNotUpdateTrackCountAfterViewIsDestroyedOnEntityLikeEvent() {
        when(likeOperations.likedTrackUrns()).thenReturn(Observable.just(likedTrackUrns));
        presenter.onViewCreated(layoutView, listView);
        presenter.onDestroyView(fragment);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(TRACK1, true, 5));

        verify(headerView, never()).updateTrackCount(anyInt());
    }

    @Test
    public void emitTrackingEventOnShuffleButtonClick() {
        when(playbackOperations.playLikedTracksShuffled(any(PlaySessionSource.class)))
                .thenReturn(Observable.<List<Urn>>empty());
        presenter.onClick(null);
        expect(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).toEqual(UIEvent.KIND_SHUFFLE_LIKES);
    }

    @Test
    public void showsDownloadedStateWhenCurrentDownloadEmitsLikedTrackDownloaded() {
        presenter.onResume(fragment);
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.downloaded(true, Arrays.asList(TRACK1)));

        verify(headerView).showDownloadedState();
    }

    @Test
    public void showsDownloadingStateWhenCurrentDownloadEmitsLikedTrackDownloading() {
        presenter.onResume(fragment);
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.downloading(TRACK1_DOWNLOAD_REQUEST));

        verify(headerView).showDownloadingState();
    }

    @Test
    public void showsDefaultStateWhenCurrentDownloadEmitsLikedTrackRequested() {
        presenter.onResume(fragment);
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.downloadRequested(true, Arrays.asList(TRACK1)));

        verify(headerView).showDefaultState();
    }

    @Test
    public void showsDefaultStateWhenCurrentDownloadEmitsLikedTrackRequestRemoved() {
        presenter.onResume(fragment);
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.downloadRequestRemoved(Arrays.asList(TRACK1_DOWNLOAD_REQUEST)));

        verify(headerView).showDefaultState();
    }

    @Test
    public void ignoresCurrentDownloadEventsWhenUnrelatedToLikedTracks() {
        presenter.onResume(fragment);
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.downloaded(false, Arrays.asList(TRACK1)));

        verifyZeroInteractions(headerView);
    }

    @Test
    public void ignoresCurrentDownloadEventsWhenOfflineContentFeatureIsDisabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        presenter.onResume(fragment);
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.downloading(TRACK1_DOWNLOAD_REQUEST));

        verifyZeroInteractions(headerView);
    }

    @Test
    public void showsDownloadedStateWhenLikedTracksDownloadStateIsDownloaded() {
        when(offlineContentOperations.getLikedTracksDownloadStateFromStorage()).thenReturn(Observable.just(DownloadState.DOWNLOADED));
        presenter.onViewCreated(layoutView, listView);

        verify(headerView).showDownloadedState();
    }

    @Test
    public void showsDefaultStateWhenLikedTracksDownloadStateIsRequested() {
        when(offlineContentOperations.getLikedTracksDownloadStateFromStorage()).thenReturn(Observable.just(DownloadState.REQUESTED));
        presenter.onViewCreated(layoutView, listView);

        verify(headerView).showDefaultState();
    }

    @Test
    public void showsDefaultStateWhenLikedTracksDownloadStateIsNoOffline() {
        when(offlineContentOperations.getLikedTracksDownloadStateFromStorage()).thenReturn(Observable.just(DownloadState.NO_OFFLINE));
        presenter.onViewCreated(layoutView, listView);

        verify(headerView).showDefaultState();
    }

    @Test
    public void doesNotShowDownloadedStateWhenOfflineContentFeatureIsDisabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        presenter.onViewCreated(layoutView, listView);

        verify(headerView, never()).showDownloadedState();
    }
}