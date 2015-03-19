package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadState;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
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
import android.view.View;
import android.widget.ListView;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class TrackLikesHeaderPresenterTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final Urn TRACK2 = Urn.forTrack(456L);
    private TrackLikesHeaderPresenter presenter;

    @Mock private TrackLikesHeaderView headerView;
    @Mock private TrackLikeOperations likeOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private OfflinePlaybackOperations playbackOperations;
    @Mock private ListBinding<PropertySet, PropertySet> listBinding;
    @Mock private Fragment fragment;
    @Mock private View layoutView;
    @Mock private ListView listView;
    private TestEventBus eventBus;
    private List<Urn> likedTrackUrns;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        presenter = new TrackLikesHeaderPresenter(headerView,
                likeOperations,
                offlineContentOperations,
                playbackOperations,
                TestSubscribers.expandPlayerSubscriber(),
                eventBus);

        likedTrackUrns = Lists.newArrayList(TRACK1, TRACK2);
    }

    @Test
    public void showHeaderDefaultStateOnRequestedState() {
        when(offlineContentOperations.getLikedTracksDownloadState()).thenReturn(Observable.just(DownloadState.REQUESTED));

        presenter.onResume(fragment);

        verify(headerView).showDefaultState();
    }

    @Test
    public void showHeaderDownloadingOnDownloadingState() {
        when(offlineContentOperations.getLikedTracksDownloadState()).thenReturn(Observable.just(DownloadState.DOWNLOADING));

        presenter.onResume(fragment);

        verify(headerView).showDownloadingState();
    }

    @Test
    public void showHeaderDownloadedStateOnDownloadedState() {
        when(offlineContentOperations.getLikedTracksDownloadState()).thenReturn(Observable.just(DownloadState.DOWNLOADED));

        presenter.onResume(fragment);

        verify(headerView).showDownloadedState();
    }

    @Test
    public void showHeaderDefaultStateOnNoOffline() {
        when(offlineContentOperations.getLikedTracksDownloadState()).thenReturn(Observable.just(DownloadState.NO_OFFLINE));

        presenter.onResume(fragment);

        verify(headerView).showDefaultState();
    }

    @Test
    public void onSubscribeListObserversUpdatesHeaderViewTrackCountOnlyOnce() {
        when(listBinding.getSource()).thenReturn(Observable.just(TestPropertySets.expectedLikedTrackForLikesScreen()).toList());
        when(likeOperations.likedTrackUrns()).thenReturn(Observable.just(likedTrackUrns));

        presenter.onViewCreated(layoutView, listView);
        presenter.onSubscribeListObservers(listBinding);

        verify(headerView).updateTrackCount(likedTrackUrns.size());
    }

    @Test
    public void doNotUpdateTrackCountAfterViewIsDestroyed() {
        when(listBinding.getSource()).thenReturn(Observable.just(TestPropertySets.expectedLikedTrackForLikesScreen()).toList());
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

}