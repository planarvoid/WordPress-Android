package com.soundcloud.android.likes;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadRequest;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ListItemAdapter;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.annotations.Issue;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
import com.soundcloud.android.tracks.TrackItem;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.ListView;

import java.util.Collections;
import java.util.List;

public class TrackLikesHeaderPresenterTest extends AndroidUnitTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final DownloadRequest TRACK1_DOWNLOAD_REQUEST = new DownloadRequest(TRACK1, 0, "http://wav", true, true, Collections.<Urn>emptyList());
    private static final Urn TRACK2 = Urn.forTrack(456L);
    private TrackLikesHeaderPresenter presenter;

    @Mock private TrackLikesHeaderView headerView;
    @Mock private TrackLikeOperations likeOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private OfflinePlaybackOperations playbackOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private LikesMenuPresenter likesMenuPresenter;
    @Mock private ListItemAdapter<TrackItem> adapter;
    @Mock private Fragment fragment;
    @Mock private View layoutView;
    @Mock private ListView listView;
    @Mock private FragmentManager fragmentManager;
    @Captor private ArgumentCaptor<View.OnClickListener> onClickListenerCaptor;

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
                eventBus,
                likesMenuPresenter);

        likedTrackUrns = asList(TRACK1, TRACK2);
        when(fragment.getFragmentManager()).thenReturn(fragmentManager);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(Observable.just(true));
        when(offlineContentOperations.getLikedTracksOfflineStateFromStorage())
                .thenReturn(Observable.just(OfflineState.NO_OFFLINE));
        when(offlineContentOperations.getOfflineLikedTracksStatusChanges()).thenReturn(Observable.just(true));
    }

    @Test
    public void onSubscribeListObserversUpdatesHeaderViewOnlyOnce() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(likeOperations.likedTrackUrns()).thenReturn(Observable.just(likedTrackUrns));
        presenter.onViewCreated(fragment, layoutView, null);

        CollectionBinding<TrackItem> collectionBinding = CollectionBinding.from(
                Observable.just(TrackItem.from(TestPropertySets.expectedLikedTrackForLikesScreen())).toList())
                .withAdapter(adapter).build();
        presenter.onSubscribeListObservers(collectionBinding);
        collectionBinding.connect();

        verify(headerView).updateTrackCount(likedTrackUrns.size());
        verify(headerView).updateOverflowMenuButton(true);
    }

    @Test
    public void doNotUpdateTrackCountAfterViewIsDestroyed() {
        PublishSubject<List<Urn>> likedTrackUrnsObservable = PublishSubject.create();
        when(likeOperations.likedTrackUrns()).thenReturn(likedTrackUrnsObservable);

        presenter.onViewCreated(fragment, layoutView, null);
        presenter.onSubscribeListObservers(CollectionBinding.from(
                Observable.just(TrackItem.from(TestPropertySets.expectedLikedTrackForLikesScreen())).toList())
                .withAdapter(adapter).build());
        presenter.onDestroyView(fragment);
        likedTrackUrnsObservable.onNext(likedTrackUrns);

        verify(headerView, never()).updateTrackCount(anyInt());
    }

    @Test
    public void updateTrackCountOnEntityLikeEvent() {
        when(likeOperations.likedTrackUrns()).thenReturn(Observable.just(likedTrackUrns));
        presenter.onViewCreated(fragment, layoutView, null);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(TRACK1, true, 5));

        verify(headerView).updateTrackCount(2);
    }

    @Test
    public void doNotUpdateTrackCountAfterViewIsDestroyedOnEntityLikeEvent() {
        when(likeOperations.likedTrackUrns()).thenReturn(Observable.just(likedTrackUrns));
        presenter.onViewCreated(fragment, layoutView, null);
        presenter.onDestroyView(fragment);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(TRACK1, true, 5));

        verify(headerView, never()).updateTrackCount(anyInt());
    }

    @Test
    public void emitTrackingEventOnShuffleButtonClick() {
        when(playbackOperations.playLikedTracksShuffled(any(PlaySessionSource.class)))
                .thenReturn(Observable.<PlaybackResult>empty());

        presenter.onViewCreated(fragment, layoutView, null);

        verify(headerView).setOnShuffleButtonClick(onClickListenerCaptor.capture());
        onClickListenerCaptor.getValue().onClick(null);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).isEqualTo(UIEvent.KIND_SHUFFLE_LIKES);
    }

    @Test
    public void showsDownloadedStateWhenCurrentDownloadEmitsLikedTrackDownloaded() {
        enableOfflineLikes();

        presenter.onResume(fragment);
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD,
                CurrentDownloadEvent.downloaded(true, Collections.singletonList(TRACK1)));

        verify(headerView, times(1)).show(OfflineState.DOWNLOADED);
    }

    @Test
    public void showsDownloadingStateWhenCurrentDownloadEmitsLikedTrackDownloading() {
        enableOfflineLikes();

        presenter.onResume(fragment);
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD,
                CurrentDownloadEvent.downloading(TRACK1_DOWNLOAD_REQUEST));

        verify(headerView).show(OfflineState.DOWNLOADING);
    }

    @Test
    public void showsRequestedStateWhenCurrentDownloadEmitsLikedTrackRequested() {
        enableOfflineLikes();

        presenter.onResume(fragment);
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD,
                CurrentDownloadEvent.downloadRequested(true, Collections.singletonList(TRACK1)));

        verify(headerView, times(2)).show(OfflineState.REQUESTED); // once from storage
    }

    @Test
    public void showsDefaultStateWhenCurrentDownloadEmitsLikedTrackRequestRemoved() {
        enableOfflineLikes();

        presenter.onResume(fragment);
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD,
                CurrentDownloadEvent.downloadRequestRemoved(Collections.singletonList(TRACK1_DOWNLOAD_REQUEST)));

        verify(headerView).show(OfflineState.NO_OFFLINE);
    }

    @Test
    public void ignoresCurrentDownloadEventsWhenUnrelatedToLikedTracks() {
        enableOfflineLikes();

        final CurrentDownloadEvent downloadNotFromLikes =
                CurrentDownloadEvent.downloaded(false, Collections.singletonList(TRACK1));

        presenter.onResume(fragment);
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD, downloadNotFromLikes);

        verify(headerView, never()).show(downloadNotFromLikes.kind);
    }

    @Test
    public void ignoresCurrentDownloadEventsWhenOfflineContentFeatureIsDisabled() {
        enableOfflineLikes();

        final CurrentDownloadEvent downloadingEvent = CurrentDownloadEvent.downloading(TRACK1_DOWNLOAD_REQUEST);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);

        presenter.onResume(fragment);
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD, downloadingEvent);

        verify(headerView, never()).show(downloadingEvent.kind);
    }

    @Test
    @Issue(ref = "https://github.com/soundcloud/SoundCloud-Android/issues/3500")
    public void ignoresCurrentDownloadEventsWhenOfflineLikesWereDisabled() {
        enableOfflineLikes();
        final CurrentDownloadEvent downloadingEvent = CurrentDownloadEvent.downloading(TRACK1_DOWNLOAD_REQUEST);
        final EntityStateChangedEvent offlineLikesDisabled = EntityStateChangedEvent.fromLikesMarkedForOffline(false);

        presenter.onViewCreated(fragment, layoutView, null);
        presenter.onResume(fragment);

        // download result being delivered after offline likes disabled
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, offlineLikesDisabled);
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD, downloadingEvent);

        verify(headerView, never()).show(downloadingEvent.kind);
    }

    @Test
    public void updatesToNoOfflineStateEvenWhenOfflineLikesDisabled() {
        enableOfflineLikes();
        final EntityStateChangedEvent offlineLikesDisabled = EntityStateChangedEvent.fromLikesMarkedForOffline(false);

        presenter.onViewCreated(fragment, layoutView, null);
        presenter.onResume(fragment);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, offlineLikesDisabled);

        verify(headerView).show(OfflineState.NO_OFFLINE);
    }

    @Test
    public void showsDownloadedStateWhenLikedTracksDownloadStateIsDownloaded() {
        when(offlineContentOperations.getLikedTracksOfflineStateFromStorage())
                .thenReturn(Observable.just(OfflineState.DOWNLOADED));
        presenter.onViewCreated(fragment, layoutView, null);
        presenter.onResume(fragment);

        verify(headerView).show(OfflineState.DOWNLOADED);
    }

    @Test
    public void showsRequestedStateWhenLikedTracksDownloadStateIsRequested() {
        when(offlineContentOperations.getLikedTracksOfflineStateFromStorage())
                .thenReturn(Observable.just(OfflineState.REQUESTED));
        presenter.onViewCreated(fragment, layoutView, null);
        presenter.onResume(fragment);

        verify(headerView).show(OfflineState.REQUESTED);
    }

    @Test
    public void showsDefaultStateWhenLikedTracksDownloadStateIsNoOffline() {
        when(offlineContentOperations.getLikedTracksOfflineStateFromStorage())
                .thenReturn(Observable.just(OfflineState.NO_OFFLINE));
        presenter.onViewCreated(fragment, layoutView, null);
        presenter.onResume(fragment);

        verify(headerView).show(OfflineState.NO_OFFLINE);
    }

    @Test
    public void removeDownloadStateWhenOfflineLikedChangeToDisable() {
        EntityStateChangedEvent likesDisabled = EntityStateChangedEvent.fromLikesMarkedForOffline(false);
        when(offlineContentOperations.getLikedTracksOfflineStateFromStorage())
                .thenReturn(Observable.just(OfflineState.DOWNLOADED));

        presenter.onResume(fragment);
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, likesDisabled);

        verify(headerView).show(OfflineState.NO_OFFLINE);
    }

    @Test
    public void doesNotShowDownloadedStateWhenOfflineContentFeatureIsDisabled() {
        enableOfflineLikes();

        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        presenter.onViewCreated(fragment, layoutView, null);
        presenter.onResume(fragment);

        verify(headerView, never()).show(OfflineState.DOWNLOADED);
    }

    @Test
    public void showOverflowMenuIfOfflineContentIsEnabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        presenter.onViewCreated(fragment, layoutView, null);
        presenter.onResume(fragment);

        verify(headerView).showOverflowMenuButton();
        verify(headerView).setOnOverflowMenuClick(any(View.OnClickListener.class));
    }

    @Test
    public void showOverflowMenuIfUpsellIsEnabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        when(featureOperations.upsellOfflineContent()).thenReturn(true);
        presenter.onViewCreated(fragment, layoutView, null);
        presenter.onResume(fragment);

        verify(headerView).showOverflowMenuButton();
        verify(headerView).setOnOverflowMenuClick(any(View.OnClickListener.class));
    }

    @Test
    public void doesNotShowOverflowMenuWhenOfflineContentIsDisabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        when(featureOperations.upsellOfflineContent()).thenReturn(false);
        presenter.onViewCreated(fragment, layoutView, null);

        verify(headerView, never()).showOverflowMenuButton();
        verify(headerView, never()).setOnOverflowMenuClick(any(View.OnClickListener.class));
    }

    @Test
    public void displayLikesMenuOnOverflowMenuClick() {
        View view = mock(View.class);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        presenter.onViewCreated(fragment, layoutView, null);
        presenter.onResume(fragment);

        verify(headerView).setOnOverflowMenuClick(onClickListenerCaptor.capture());
        onClickListenerCaptor.getValue().onClick(view);

        verify(likesMenuPresenter).show(view, fragmentManager);
    }

    private void enableOfflineLikes() {
        when(offlineContentOperations.getLikedTracksOfflineStateFromStorage())
                .thenReturn(Observable.just(OfflineState.REQUESTED));
    }

}