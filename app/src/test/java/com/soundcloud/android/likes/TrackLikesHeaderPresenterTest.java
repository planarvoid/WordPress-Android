package com.soundcloud.android.likes;

import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloaded;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloading;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.removed;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.requested;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineLikesDialog;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.offline.OfflineStateOperations;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.ListItemAdapter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.annotations.Issue;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.ListView;

import java.util.Arrays;
import java.util.List;

public class TrackLikesHeaderPresenterTest extends AndroidUnitTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final Urn TRACK2 = Urn.forTrack(456L);

    private TrackLikesHeaderPresenter presenter;

    @Mock private TrackLikesHeaderViewFactory headerViewFactory;
    @Mock private TrackLikesHeaderView headerView;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private OfflineStateOperations offlineStateOperations;
    @Mock private TrackLikeOperations likeOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private OfflineLikesDialog offlineLikesDialog;
    @Mock private Navigator navigator;

    @Mock private ListItemAdapter<TrackItem> adapter;
    @Mock private Fragment fragment;
    @Mock private View layoutView;
    @Mock private ListView listView;
    @Mock private FragmentManager fragmentManager;

    private TestEventBus eventBus;
    private List<Urn> likedTrackUrns;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        presenter = new TrackLikesHeaderPresenter(
                headerViewFactory,
                offlineContentOperations,
                offlineStateOperations,
                likeOperations,
                featureOperations,
                playbackInitiator,
                TestSubscribers.expandPlayerSubscriber(),
                InjectionSupport.providerOf(offlineLikesDialog),
                navigator,
                eventBus);

        likedTrackUrns = asList(TRACK1, TRACK2);
        when(fragment.getFragmentManager()).thenReturn(fragmentManager);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(offlineStateOperations.loadLikedTracksOfflineState())
                .thenReturn(just(OfflineState.NOT_OFFLINE));
        when(offlineContentOperations.getOfflineLikedTracksStatusChanges()).thenReturn(just(true));
        when(headerViewFactory.create(any(View.class), any(TrackLikesHeaderView.Listener.class))).thenReturn(headerView);
    }

    @Test
    public void updateTrackCountUpdatesTrackCountWhenViewIsReady() {
        presenter.onViewCreated(fragment, layoutView, null);

        presenter.updateTrackCount(3);

        verify(headerView).updateTrackCount(3);
    }

    @Test
    public void doesNotUpdateTrackCountBeforeViewIsReady() {
        presenter.updateTrackCount(3);

        verify(headerView, never()).updateTrackCount(3);
    }

    @Test
    public void doNotUpdateTrackCountAfterViewIsDestroyedOnEntityLikeEvent() {
        when(likeOperations.likedTrackUrns()).thenReturn(just(likedTrackUrns));
        presenter.onViewCreated(fragment, layoutView, null);
        presenter.onDestroyView(fragment);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(TRACK1, true, 5));

        verify(headerView, never()).updateTrackCount(anyInt());
    }

    @Test
    public void emitTrackingEventOnShuffleButtonClick() {
        when(playbackInitiator.playTracksShuffled(any(Observable.class), any(PlaySessionSource.class)))
                .thenReturn(Observable.<PlaybackResult>empty());
        presenter.onViewCreated(fragment, layoutView, null);

        presenter.onShuffle();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).isEqualTo(UIEvent.KIND_SHUFFLE_LIKES);
    }

    @Test
    public void showsDownloadedStateWhenCurrentDownloadEmitsLikedTrackDownloaded() {
        enableOfflineLikes();
        presenter.onViewCreated(fragment, layoutView, null);

        presenter.onResume(fragment);
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, downloaded(singletonList(TRACK1), true));

        final InOrder inOrder = inOrder(headerView);
        inOrder.verify(headerView).show(OfflineState.REQUESTED);
        inOrder.verify(headerView).show(OfflineState.DOWNLOADED);
    }

    @Test
    public void showsDownloadingStateWhenCurrentDownloadEmitsLikedTrackDownloading() {
        enableOfflineLikes();
        presenter.onViewCreated(fragment, layoutView, null);

        presenter.onResume(fragment);
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, downloading(singletonList(TRACK1), true));

        verify(headerView).show(OfflineState.DOWNLOADING);
    }

    @Test
    public void showsRequestedStateWhenCurrentDownloadEmitsLikedTrackRequested() {
        enableOfflineLikes();
        presenter.onViewCreated(fragment, layoutView, null);

        presenter.onResume(fragment);
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, requested(singletonList(TRACK1), true));

        verify(headerView, times(2)).show(OfflineState.REQUESTED); // once from storage
    }

    @Test
    public void showsDefaultStateWhenCurrentDownloadEmitsLikedTrackRequestRemoved() {
        enableOfflineLikes();
        presenter.onViewCreated(fragment, layoutView, null);

        presenter.onResume(fragment);
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, removed(true));

        verify(headerView).show(OfflineState.NOT_OFFLINE);
    }

    @Test
    public void ignoresCurrentDownloadEventsWhenUnrelatedToLikedTracks() {
        enableOfflineLikes();
        presenter.onViewCreated(fragment, layoutView, null);
        final OfflineContentChangedEvent downloadNotFromLikes = downloaded(singletonList(TRACK1), false);

        presenter.onResume(fragment);
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, downloadNotFromLikes);

        verify(headerView, never()).show(downloadNotFromLikes.state);
    }

    @Test
    public void ignoresCurrentDownloadEventsWhenOfflineContentFeatureIsDisabled() {
        enableOfflineLikes();
        presenter.onViewCreated(fragment, layoutView, null);
        final OfflineContentChangedEvent downloadingEvent = downloading(singletonList(TRACK1), true);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);

        presenter.onResume(fragment);
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, downloadingEvent);

        verify(headerView, never()).show(downloadingEvent.state);
    }

    @Test
    @Issue(ref = "https://github.com/soundcloud/SoundCloud-Android/issues/3500")
    public void ignoresCurrentDownloadEventsWhenOfflineLikesWereDisabled() {
        enableOfflineLikes();
        final OfflineContentChangedEvent downloadingEvent = downloading(Arrays.asList(TRACK1, Urn.forPlaylist(123L)), false);
        final OfflineContentChangedEvent offlineLikesDisabled = removed(true);
        presenter.onViewCreated(fragment, layoutView, null);
        presenter.onResume(fragment);

        // download result being delivered after offline likes disabled
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, offlineLikesDisabled);
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, downloadingEvent);

        verify(headerView, never()).show(downloadingEvent.state);
    }

    @Test
    public void updatesToNoOfflineStateEvenWhenOfflineLikesDisabled() {
        enableOfflineLikes();

        presenter.onViewCreated(fragment, layoutView, null);
        presenter.onResume(fragment);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, removed(true));

        final InOrder inOrder = inOrder(headerView);
        inOrder.verify(headerView).show(OfflineState.REQUESTED);
        inOrder.verify(headerView).show(OfflineState.NOT_OFFLINE);
    }

    @Test
    public void showsDownloadedStateWhenLikedTracksDownloadStateIsDownloaded() {
        when(featureOperations.isOfflineContentOrUpsellEnabled()).thenReturn(true);
        when(offlineStateOperations.loadLikedTracksOfflineState())
                .thenReturn(just(OfflineState.DOWNLOADED));
        presenter.onViewCreated(fragment, layoutView, null);
        presenter.onResume(fragment);

        verify(headerView).show(OfflineState.DOWNLOADED);
    }

    @Test
    public void showsRequestedStateWhenLikedTracksDownloadStateIsRequested() {
        when(featureOperations.isOfflineContentOrUpsellEnabled()).thenReturn(true);
        when(offlineStateOperations.loadLikedTracksOfflineState())
                .thenReturn(just(OfflineState.REQUESTED));
        presenter.onViewCreated(fragment, layoutView, null);
        presenter.onResume(fragment);

        verify(headerView).show(OfflineState.REQUESTED);
    }

    @Test
    public void showsDefaultStateWhenLikedTracksDownloadStateIsNoOffline() {
        when(offlineStateOperations.loadLikedTracksOfflineState())
                .thenReturn(just(OfflineState.NOT_OFFLINE));
        presenter.onViewCreated(fragment, layoutView, null);
        presenter.onResume(fragment);

        verify(headerView).show(OfflineState.NOT_OFFLINE);
    }

    @Test
    public void removeDownloadStateWhenOfflineLikedChangeToDisable() {
        final OfflineContentChangedEvent offlineLikesDisabled = removed(true);
        when(offlineStateOperations.loadLikedTracksOfflineState())
                .thenReturn(just(OfflineState.DOWNLOADED));
        presenter.onViewCreated(fragment, layoutView, null);

        presenter.onResume(fragment);
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, offlineLikesDisabled);

        verify(headerView).show(OfflineState.NOT_OFFLINE);
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
    public void showsSyncLikesDialogWhenOfflineLikesEnabled() {
        presenter.onViewCreated(fragment, layoutView, null);

        presenter.onMakeAvailableOffline(true);

        verify(offlineLikesDialog).show(any(FragmentManager.class));
    }

    @Test
    public void disablesLikesSyncingWhenOfflineLikesDisabled() {
        when(offlineContentOperations.disableOfflineLikedTracks()).thenReturn(Observable.<Void>just(null));
        presenter.onViewCreated(fragment, layoutView, null);

        presenter.onMakeAvailableOffline(false);

        verify(offlineContentOperations).disableOfflineLikedTracks();
        verifyZeroInteractions(offlineLikesDialog);
    }

    @Test
    public void opensUpgradeFlowOnUpsellClick() {
        presenter.onViewCreated(fragment, layoutView, null);

        presenter.onUpsell();

        verify(navigator).openUpgrade(any(Activity.class));
    }

    @Test
    public void showsOfflineDownloadOptionWhenOfflineLikesDisabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(offlineContentOperations.getOfflineLikedTracksStatusChanges()).thenReturn(Observable.just(false));
        presenter.onViewCreated(fragment, layoutView, null);

        presenter.onResume(fragment);

        verify(headerView).setDownloadedButtonState(false);
    }

    @Test
    public void showsOfflineRemovalOptionWhenOfflineTracksEnabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(offlineContentOperations.getOfflineLikedTracksStatusChanges()).thenReturn(Observable.just(true));
        presenter.onViewCreated(fragment, layoutView, null);

        presenter.onResume(fragment);

        verify(headerView).setDownloadedButtonState(true);
    }

    @Test
    public void neverShowsDownloadButtonIfOfflineAndUpsellUnavailable() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        when(featureOperations.upsellOfflineContent()).thenReturn(false);
        presenter.onViewCreated(fragment, layoutView, null);

        presenter.onResume(fragment);

        verify(headerView, never()).setDownloadedButtonState(anyBoolean());
    }

    @Test
    public void sendsTrackingEventWhenRemovingOfflineLikes() {
        when(offlineContentOperations.disableOfflineLikedTracks()).thenReturn(Observable.<Void>empty());
        when(offlineContentOperations.isOfflineCollectionEnabled()).thenReturn(false);
        presenter.onViewCreated(fragment, layoutView, null);

        presenter.onMakeAvailableOffline(false);

        OfflineInteractionEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING, OfflineInteractionEvent.class);
        assertThat(trackingEvent.getKind()).isEqualTo(OfflineInteractionEvent.KIND_OFFLINE_LIKES_REMOVE);
        assertThat(trackingEvent.getAttributes().containsValue(Screen.LIKES.get())).isTrue();
    }

    private void enableOfflineLikes() {
        when(featureOperations.isOfflineContentOrUpsellEnabled()).thenReturn(true);
        when(offlineStateOperations.loadLikedTracksOfflineState()).thenReturn(just(OfflineState.REQUESTED));
    }

}
