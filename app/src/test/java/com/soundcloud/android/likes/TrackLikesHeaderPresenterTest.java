package com.soundcloud.android.likes;

import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloaded;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloading;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.removed;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.requested;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.GoOnboardingTooltipExperiment;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.TrackLikesHeaderPresenter.UpdateHeaderViewSubscriber;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineLikesDialog;
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.offline.OfflineStateOperations;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.ListItemAdapter;
import com.soundcloud.android.settings.OfflineStorageErrorDialog;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.annotations.Issue;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.ConnectionHelper;
import com.soundcloud.rx.eventbus.TestEventBus;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.ListView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TrackLikesHeaderPresenterTest extends AndroidUnitTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final Urn TRACK2 = Urn.forTrack(456L);

    @Mock private TrackLikesHeaderViewFactory headerViewFactory;
    @Mock private TrackLikesHeaderView headerView;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private OfflineStateOperations offlineStateOperations;
    @Mock private TrackLikeOperations likeOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private OfflineLikesDialog offlineLikesDialog;
    @Mock private ConnectionHelper connectionHelper;
    @Mock private OfflineSettingsOperations offlineSettings;
    @Mock private NavigationExecutor navigationExecutor;
    @Mock private OfflineSettingsStorage offlineSettingsStorage;
    @Mock private ListItemAdapter<TrackItem> adapter;
    @Mock private Fragment fragment;
    @Mock private View layoutView;
    @Mock private ListView listView;
    @Mock private FragmentManager fragmentManager;
    @Mock private FragmentTransaction fragmentTransaction;
    @Mock private GoOnboardingTooltipExperiment goOnboardingTooltipExperiment;

    private TrackLikesHeaderPresenter presenter;
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
                navigationExecutor,
                eventBus,
                InjectionSupport.providerOf(new UpdateHeaderViewSubscriber(offlineSettings, connectionHelper, eventBus)),
                offlineSettingsStorage,
                goOnboardingTooltipExperiment);

        likedTrackUrns = asList(TRACK1, TRACK2);
        when(fragmentManager.beginTransaction()).thenReturn(fragmentTransaction);
        when(fragment.getActivity()).thenReturn(activity());
        when(fragment.getFragmentManager()).thenReturn(fragmentManager);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(offlineStateOperations.loadLikedTracksOfflineState())
                .thenReturn(Observable.just(OfflineState.NOT_OFFLINE));
        when(offlineContentOperations.getOfflineLikedTracksStatusChanges()).thenReturn(rx.Observable.just(true));
        when(headerViewFactory.create(any(View.class),
                                      any(TrackLikesHeaderView.Listener.class))).thenReturn(headerView);
        when(connectionHelper.isNetworkConnected()).thenReturn(true);
        when(offlineSettingsStorage.isOfflineContentAccessible()).thenReturn(true);
    }

    @Test
    public void updateTrackCountUpdatesTrackCountWhenViewIsReady() {
        createAndBindView();

        presenter.updateTrackCount(3);

        verify(headerView).updateTrackCount(3);
    }

    @Test
    public void doesNotUpdateTrackCountBeforeViewIsReady() {
        presenter.onCreate(fragment, null);
        presenter.updateTrackCount(3);

        verify(headerView, never()).updateTrackCount(3);
    }

    @Test
    public void emitTrackingEventOnShuffleButtonClick() {
        when(likeOperations.likedTrackUrns()).thenReturn(Single.just(likedTrackUrns));
        when(playbackInitiator.playTracksShuffled(any(rx.Observable.class), any(PlaySessionSource.class)))
                .thenReturn(rx.Observable.empty());
        createAndBindView();

        presenter.onShuffle();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).isEqualTo(UIEvent.Kind.SHUFFLE.toString());
    }

    @Test
    public void showsDownloadedStateWhenCurrentDownloadEmitsLikedTrackDownloaded() {
        enableOfflineLikes();
        createAndBindView();

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, downloaded(singletonList(TRACK1), true));

        final InOrder inOrder = inOrder(headerView);
        inOrder.verify(headerView).show(OfflineState.REQUESTED);
        inOrder.verify(headerView).show(OfflineState.DOWNLOADED);
    }

    @Test
    public void showsDownloadingStateWhenCurrentDownloadEmitsLikedTrackDownloading() {
        enableOfflineLikes();
        createAndBindView();

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, downloading(singletonList(TRACK1), true));

        verify(headerView).show(OfflineState.DOWNLOADING);
    }

    @Test
    public void showsRequestedStateWhenCurrentDownloadEmitsLikedTrackRequested() {
        enableOfflineLikes();
        createAndBindView();

        presenter.onResume(fragment);
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, requested(singletonList(TRACK1), true));

        verify(headerView, times(2)).show(OfflineState.REQUESTED); // once from storage
    }

    @Test
    public void showsDefaultStateWhenCurrentDownloadEmitsLikedTrackRequestRemoved() {
        enableOfflineLikes();
        createAndBindView();

        presenter.onResume(fragment);
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, removed(true));

        verify(headerView).show(OfflineState.NOT_OFFLINE);
    }

    @Test
    public void ignoresCurrentDownloadEventsWhenUnrelatedToLikedTracks() {
        enableOfflineLikes();
        createAndBindView();

        final OfflineContentChangedEvent downloadNotFromLikes = downloaded(singletonList(TRACK1), false);

        presenter.onResume(fragment);
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, downloadNotFromLikes);

        verify(headerView, never()).show(downloadNotFromLikes.state);
    }

    @Test
    public void ignoresCurrentDownloadEventsWhenOfflineContentFeatureIsDisabled() {
        enableOfflineLikes();
        createAndBindView();

        final OfflineContentChangedEvent downloadingEvent = downloading(singletonList(TRACK1), true);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, downloadingEvent);

        verify(headerView, never()).show(downloadingEvent.state);
    }

    @Test
    @Issue(ref = "https://github.com/soundcloud/android-listeners/issues/3500")
    public void ignoresCurrentDownloadEventsWhenOfflineLikesWereDisabled() {
        enableOfflineLikes();
        final OfflineContentChangedEvent downloadingEvent = downloading(Arrays.asList(TRACK1, Urn.forPlaylist(123L)),
                                                                        false);
        final OfflineContentChangedEvent offlineLikesDisabled = removed(true);
        createAndBindView();

        // download result being delivered after offline likes disabled
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, offlineLikesDisabled);
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, downloadingEvent);

        verify(headerView, never()).show(downloadingEvent.state);
    }

    @Test
    public void updatesToNoOfflineStateEvenWhenOfflineLikesDisabled() {
        enableOfflineLikes();

        createAndBindView();

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, removed(true));

        final InOrder inOrder = inOrder(headerView);
        inOrder.verify(headerView).show(OfflineState.REQUESTED);
        inOrder.verify(headerView).show(OfflineState.NOT_OFFLINE);
    }

    @Test
    public void showsDownloadedStateWhenLikedTracksDownloadStateIsDownloaded() {
        when(offlineStateOperations.loadLikedTracksOfflineState())
                .thenReturn(Observable.just(OfflineState.DOWNLOADED));
        createAndBindView();

        verify(headerView).show(OfflineState.DOWNLOADED);
    }

    @Test
    public void showsRequestedStateWhenLikedTracksDownloadStateIsRequested() {
        when(offlineStateOperations.loadLikedTracksOfflineState())
                .thenReturn(Observable.just(OfflineState.REQUESTED));
        createAndBindView();

        verify(headerView).show(OfflineState.REQUESTED);
    }

    @Test
    public void showsDefaultStateWhenLikedTracksDownloadStateIsNoOffline() {
        when(offlineStateOperations.loadLikedTracksOfflineState())
                .thenReturn(Observable.just(OfflineState.NOT_OFFLINE));
        createAndBindView();

        verify(headerView).show(OfflineState.NOT_OFFLINE);
    }

    @Test
    public void removeDownloadStateWhenOfflineLikedChangeToDisable() {
        final OfflineContentChangedEvent offlineLikesDisabled = removed(true);
        when(offlineStateOperations.loadLikedTracksOfflineState())
                .thenReturn(Observable.just(OfflineState.DOWNLOADED));
        createAndBindView();

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, offlineLikesDisabled);

        verify(headerView).show(OfflineState.NOT_OFFLINE);
    }

    @Test
    public void doesNotShowDownloadedStateWhenOfflineContentFeatureIsDisabled() {
        enableOfflineLikes();

        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        createAndBindView();

        verify(headerView, never()).show(OfflineState.DOWNLOADED);
    }

    @Test
    public void enablesOfflineLikesWithoutDialogWhenExperimentEnabled() {
        when(offlineContentOperations.enableOfflineLikedTracks()).thenReturn(rx.Observable.just(null));
        when(goOnboardingTooltipExperiment.isEnabled()).thenReturn(true);

        createAndBindView();

        presenter.onMakeAvailableOffline(true);

        verify(offlineContentOperations).enableOfflineLikedTracks();
        verifyZeroInteractions(offlineLikesDialog);
    }

    @Test
    public void showsSyncLikesDialogWhenOfflineLikesEnabled() {
        createAndBindView();

        presenter.onMakeAvailableOffline(true);

        verify(offlineLikesDialog).show(any(FragmentManager.class));
    }

    @Test
    public void disablesLikesSyncingWhenOfflineLikesDisabled() {
        when(offlineContentOperations.disableOfflineLikedTracks()).thenReturn(rx.Observable.just(null));
        createAndBindView();

        presenter.onMakeAvailableOffline(false);

        verify(offlineContentOperations).disableOfflineLikedTracks();
        verifyZeroInteractions(offlineLikesDialog);
    }

    @Test
    public void showWarningTextWhenPendingDownloadAndWifiOnly() {
        when(offlineStateOperations.loadLikedTracksOfflineState())
                .thenReturn(Observable.just(OfflineState.REQUESTED));
        when(offlineSettings.isWifiOnlyEnabled()).thenReturn(true);
        when(connectionHelper.isWifiConnected()).thenReturn(false);

        createAndBindView();

        verify(headerView).showNoWifi();
    }

    @Test
    public void showWarningTextWhenPendingDownloadAndOffline() {
        when(offlineStateOperations.loadLikedTracksOfflineState())
                .thenReturn(Observable.just(OfflineState.REQUESTED));
        when(connectionHelper.isNetworkConnected()).thenReturn(false);
        createAndBindView();

        verify(headerView).showNoConnection();
    }

    @Test
    public void doNotShowWarningTextForNonPendingStates() {
        when(offlineStateOperations.loadLikedTracksOfflineState())
                .thenReturn(Observable.just(OfflineState.DOWNLOADED));
        when(connectionHelper.isNetworkConnected()).thenReturn(false);
        createAndBindView();

        verify(headerView, times(0)).showNoConnection();
    }

    @Test
    public void doNotShowIntroductoryOverlayIfLikesEnabled() {
        when(offlineContentOperations.getOfflineLikedTracksStatusChanges()).thenReturn(rx.Observable.just(true));
        when(connectionHelper.isNetworkConnected()).thenReturn(true);
        createAndBindView();

        verify(headerView, never()).showOfflineIntroductoryOverlay();
    }

    @Test
    public void doNotShowIntroductoryOverlayIfNoConnection() {
        when(offlineContentOperations.getOfflineLikedTracksStatusChanges()).thenReturn(rx.Observable.just(false));
        when(connectionHelper.isNetworkConnected()).thenReturn(false);
        createAndBindView();

        verify(headerView, never()).showOfflineIntroductoryOverlay();
    }

    @Test
    public void showIntroductoryOverlayIfLikesNotEnabledAndHasConnection() {
        when(offlineContentOperations.getOfflineLikedTracksStatusChanges()).thenReturn(rx.Observable.just(false));
        when(connectionHelper.isNetworkConnected()).thenReturn(true);
        createAndBindView();

        verify(headerView).showOfflineIntroductoryOverlay();
    }

    @Test
    public void opensUpgradeFlowOnUpsellClick() {
        presenter.onViewCreated(fragment, layoutView, null);
        presenter.bindItemView(0, layoutView, Collections.emptyList());

        presenter.onUpsell();

        verify(navigationExecutor).openUpgrade(any(Activity.class), eq(UpsellContext.OFFLINE));
    }

    @Test
    public void showsOfflineDownloadOptionWhenOfflineLikesDisabled() {
        when(offlineContentOperations.getOfflineLikedTracksStatusChanges()).thenReturn(rx.Observable.just(false));
        createAndBindView();

        verify(headerView).setDownloadedButtonState(false);
    }

    @Test
    public void showsOfflineRemovalOptionWhenOfflineTracksEnabled() {
        when(offlineContentOperations.getOfflineLikedTracksStatusChanges()).thenReturn(rx.Observable.just(true));
        createAndBindView();

        verify(headerView).setDownloadedButtonState(true);
    }

    @Test
    public void handlesOfflineContentNotAccessible() {
        when(offlineSettingsStorage.isOfflineContentAccessible()).thenReturn(false);

        createAndBindView();
        presenter.onMakeAvailableOffline(true);

        verify(fragmentTransaction).add(any(OfflineStorageErrorDialog.class), anyString());
        verify(fragmentTransaction).commit();
        verify(offlineLikesDialog, never()).show(any(FragmentManager.class));
    }

    @Test
    public void neverShowsDownloadButtonIfOfflineAndUpsellUnavailable() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        when(featureOperations.upsellOfflineContent()).thenReturn(false);
        createAndBindView();

        verify(headerView, never()).setDownloadedButtonState(anyBoolean());
    }

    @Test
    public void sendsTrackingEventWhenRemovingOfflineLikes() {
        when(offlineContentOperations.disableOfflineLikedTracks()).thenReturn(rx.Observable.empty());
        when(offlineContentOperations.isOfflineCollectionEnabled()).thenReturn(false);
        createAndBindView();

        presenter.onMakeAvailableOffline(false);

        OfflineInteractionEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING,
                                                                     OfflineInteractionEvent.class);
        assertThat(trackingEvent.offlineContentContext().get()).isEqualTo(OfflineInteractionEvent.OfflineContentContext.LIKES_CONTEXT);
        assertThat(trackingEvent.isEnabled().get()).isEqualTo(false);
        assertThat(trackingEvent.clickName().get()).isEqualTo(OfflineInteractionEvent.Kind.KIND_OFFLINE_LIKES_REMOVE);
        assertThat(trackingEvent.pageName().get()).isEqualTo(Screen.LIKES.get());
    }

    private void createAndBindView() {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, layoutView, null);
        presenter.bindItemView(0, layoutView, Collections.emptyList());
    }

    private void enableOfflineLikes() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(offlineStateOperations.loadLikedTracksOfflineState()).thenReturn(Observable.just(OfflineState.REQUESTED));
    }
}
