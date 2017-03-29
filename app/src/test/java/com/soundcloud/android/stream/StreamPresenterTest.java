package com.soundcloud.android.stream;

import static com.soundcloud.android.playback.VideoSurfaceProvider.Origin;
import static com.soundcloud.android.stream.StreamItem.forFacebookListenerInvites;
import static com.soundcloud.android.testsupport.fixtures.PlayableFixtures.expectedLikedPlaylistForPlaylistsScreen;
import static com.soundcloud.android.testsupport.fixtures.PlayableFixtures.expectedPromotedTrack;
import static com.soundcloud.android.testsupport.fixtures.PlayableFixtures.expectedTrackForListItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AppInstallAd;
import com.soundcloud.android.ads.StreamAdsController;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.ads.WhyAdsDialogPresenter;
import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.discovery.DefaultHomeScreenConfiguration;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.FacebookInvitesEvent;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.StreamEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.facebookinvites.FacebookInvitesDialogPresenter;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayableWithReposter;
import com.soundcloud.android.playback.VideoSurfaceProvider;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.Assertions;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.TestPager;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.UpdatePlayableAdapterSubscriber;
import com.soundcloud.android.tracks.UpdatePlayableAdapterSubscriberFactory;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.view.NewItemsIndicator;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.subjects.PublishSubject;

import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.TextureView;
import android.view.View;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class StreamPresenterTest extends AndroidUnitTest {

    private static final Date CREATED_AT = new Date();

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh);
    private final Date DATE = new Date(123L);

    private StreamPresenter presenter;

    @Mock private StreamOperations streamOperations;
    @Mock private StreamAdapter adapter;
    @Mock private ImagePauseOnScrollListener imagePauseOnScrollListener;
    @Mock private StreamAdsController streamAdsController;
    @Mock private StreamDepthPublisherFactory streamDepthPublisherFactory;
    @Mock private StreamDepthPublisher streamDepthPublisher;
    @Mock private StreamSwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private DateProvider dateProvider;
    @Mock private Observer<Iterable<StreamItem>> itemObserver;
    @Mock private MixedItemClickListener.Factory itemClickListenerFactory;
    @Mock private MixedItemClickListener itemClickListener;
    @Mock private Navigator navigator;
    @Mock private FacebookInvitesDialogPresenter facebookInvitesDialogPresenter;
    @Mock private StationsOperations stationsOperations;
    @Mock private View view;
    @Mock private NewItemsIndicator newItemsIndicator;
    @Mock private FollowingOperations followingOperations;
    @Mock private UpdatePlayableAdapterSubscriberFactory updatePlayableAdapterSubscriberFactory;
    @Mock private WhyAdsDialogPresenter whyAdsDialogPresenter;
    @Mock private VideoSurfaceProvider videoSurfaceProvider;
    @Mock private TextureView textureView;
    @Mock private DefaultHomeScreenConfiguration defaultHomeScreenConfiguration;
    @Mock private PerformanceMetricsEngine performanceMetricsEngine;

    @Captor private ArgumentCaptor<PerformanceMetric> performanceMetricArgumentCaptor;

    private UpdatePlayableAdapterSubscriber updatePlayableAdapterSubscriber;
    private TestEventBus eventBus = new TestEventBus();
    private PublishSubject<Urn> followSubject = PublishSubject.create();
    private PublishSubject<Urn> unfollowSubject = PublishSubject.create();

    @Before
    public void setUp() throws Exception {

        updatePlayableAdapterSubscriber = spy(new UpdatePlayableAdapterSubscriber(adapter));
        when(updatePlayableAdapterSubscriberFactory.create(adapter)).thenReturn(updatePlayableAdapterSubscriber);
        when(itemClickListenerFactory.create(Screen.STREAM, null)).thenReturn(itemClickListener);
        presenter = new StreamPresenter(
                streamOperations,
                adapter,
                stationsOperations,
                imagePauseOnScrollListener,
                streamAdsController,
                streamDepthPublisherFactory,
                eventBus,
                itemClickListenerFactory,
                swipeRefreshAttacher,
                facebookInvitesDialogPresenter,
                navigator,
                newItemsIndicator,
                followingOperations,
                whyAdsDialogPresenter,
                videoSurfaceProvider,
                updatePlayableAdapterSubscriberFactory,
                defaultHomeScreenConfiguration,
                performanceMetricsEngine);

        when(streamOperations.initialStreamItems()).thenReturn(Observable.empty());
        when(streamOperations.updatedTimelineItemsForStart()).thenReturn(Observable.empty());
        when(streamOperations.pagingFunction()).thenReturn(TestPager.singlePageFunction());
        when(dateProvider.getCurrentTime()).thenReturn(100L);
        when(followingOperations.onUserFollowed()).thenReturn(followSubject);
        when(followingOperations.onUserUnfollowed()).thenReturn(unfollowSubject);
        when(streamDepthPublisherFactory.create(any(StaggeredGridLayoutManager.class), anyBoolean())).thenReturn(streamDepthPublisher);
        when(streamAdsController.isInFullscreen()).thenReturn(false);
    }

    @Test
    public void canLoadStreamItems() {
        final TrackItem promotedTrackItem = expectedPromotedTrack();
        TrackStreamItem promotedTrackStreamItem = TrackStreamItem.create(promotedTrackItem, CREATED_AT, Optional.absent());
        final TrackItem trackItem = expectedTrackForListItem(Urn.forTrack(123L));
        TrackStreamItem normalTrackStreamItem = TrackStreamItem.create(trackItem, CREATED_AT, Optional.absent());
        final PlaylistItem playlistItem = expectedLikedPlaylistForPlaylistsScreen();
        PlaylistStreamItem playlistStreamItem = PlaylistStreamItem.create(playlistItem, CREATED_AT, Optional.absent());
        final List<StreamItem> items = Arrays.asList(promotedTrackStreamItem,
                                                     normalTrackStreamItem,
                                                     playlistStreamItem);
        when(streamOperations.initialStreamItems()).thenReturn(Observable.just(items));

        CollectionBinding<List<StreamItem>, StreamItem> binding = presenter.onBuildBinding(null);
        binding.connect();
        binding.items().subscribe(itemObserver);

        verify(itemObserver).onNext(items);
    }

    @Test
    public void canRefreshStreamItems() {
        final TrackItem trackItem = expectedTrackForListItem(Urn.forTrack(123L));
        final StreamItem streamItem = TrackStreamItem.create(trackItem, CREATED_AT, Optional.absent());
        when(streamOperations.updatedStreamItems()).thenReturn(Observable.just(
                Collections.singletonList(streamItem)
        ));

        CollectionBinding<List<StreamItem>, StreamItem> binding = presenter.onRefreshBinding();
        binding.connect();
        binding.items().subscribe(itemObserver);

        verify(itemObserver).onNext(Collections.singletonList(streamItem));
    }

    @Test
    public void forwardsTrackClicksToClickListener() {
        final TrackItem clickedTrack = ModelFixtures.trackItem();
        final Observable<List<PlayableWithReposter>> streamTracks = Observable.just(Arrays.asList(PlayableWithReposter.from(clickedTrack.getUrn()), PlayableWithReposter.from(Urn.forTrack(634L))));

        when(adapter.getItem(0)).thenReturn(TrackStreamItem.create(clickedTrack, clickedTrack.getCreatedAt(), Optional.absent()));
        when(streamOperations.urnsForPlayback()).thenReturn(streamTracks);

        presenter.onItemClicked(view, 0);

        verify(itemClickListener).legacyOnPostClick(streamTracks, view, 0, clickedTrack);
    }

    @Test
    public void tracksPromotedTrackItemClick() {
        final TrackItem clickedTrack = expectedPromotedTrack();
        final Observable<List<PlayableWithReposter>> streamTracks = Observable.just(Arrays.asList(PlayableWithReposter.from(clickedTrack.getUrn()), PlayableWithReposter.from(Urn.forTrack(634L))));

        when(adapter.getItem(0)).thenReturn(TrackStreamItem.create(clickedTrack, clickedTrack.getCreatedAt(), Optional.absent()));
        when(streamOperations.urnsForPlayback()).thenReturn(streamTracks);

        presenter.onItemClicked(view, 0);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(PromotedTrackingEvent.class);
    }

    @Test
    public void tracksPromotedPlaylistItemClick() {
        final PlaylistItem clickedPlaylist = PlayableFixtures.expectedPromotedPlaylist();
        final Observable<List<PlayableWithReposter>> streamTracks = Observable.just(Arrays.asList(PlayableWithReposter.from(clickedPlaylist.getUrn()), PlayableWithReposter.from(Urn.forTrack(634L))));

        when(adapter.getItem(0)).thenReturn(PlaylistStreamItem.create(clickedPlaylist, clickedPlaylist.getCreatedAt(), Optional.absent()));
        when(streamOperations.urnsForPlayback()).thenReturn(streamTracks);

        presenter.onItemClicked(view, 0);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(PromotedTrackingEvent.class);
        verify(itemClickListener).legacyOnPostClick(streamTracks, view, 0, clickedPlaylist);
    }

    @Test
    public void forwardsPlaylistClicksToClickListener() {
        final PlaylistItem playlistItem = ModelFixtures.playlistItem();
        final Observable<List<PlayableWithReposter>> streamTracks = Observable.just(Arrays.asList(PlayableWithReposter.from(playlistItem.getUrn()), PlayableWithReposter.from(Urn.forTrack(634L))));

        when(adapter.getItem(0)).thenReturn(PlaylistStreamItem.create(playlistItem, playlistItem.getCreatedAt(), Optional.absent()));
        when(streamOperations.urnsForPlayback()).thenReturn(streamTracks);

        presenter.onItemClicked(view, 0);

        verify(itemClickListener).legacyOnPostClick(streamTracks, view, 0, playlistItem);
    }

    @Test
    public void unsubscribesFromEventBusOnDestroyView() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onDestroyView(fragmentRule.getFragment());

        eventBus.verifyUnsubscribed();
    }

    @Test
    public void trackChangedEventUpdatesCurrentlyPlayingTrack() {
        final Urn playingTrack = Urn.forTrack(123L);
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        final CurrentPlayQueueItemEvent event = CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(playingTrack), Urn.NOT_SET, 0);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         event);


        verify(updatePlayableAdapterSubscriber).onNext(event);
    }

    @Test
    public void newQueueEventUpdatesCurrentlyPlayingTrack() {
        final Urn playingTrack = Urn.forTrack(123L);
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        final CurrentPlayQueueItemEvent event = CurrentPlayQueueItemEvent.fromNewQueue(TestPlayQueueItem.createTrack(playingTrack),
                                                                                       Urn.NOT_SET,
                                                                                       0);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         event);

        verify(updatePlayableAdapterSubscriber).onNext(event);
    }

    @Test
    public void shouldPublishTrackingEventOnFacebookInvitesButtonClick() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        when(adapter.getItem(0)).thenReturn(forFacebookListenerInvites());

        presenter.onListenerInvitesClicked(0);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(FacebookInvitesEvent.class);
    }

    @Test
    public void shouldOpenFacebookInvitesDialogOnFacebookInvitesButtonClick() {
        when(adapter.getItem(0)).thenReturn(forFacebookListenerInvites());
        presenter.onCreate(fragmentRule.getFragment(), null);

        presenter.onListenerInvitesClicked(0);

        verify(facebookInvitesDialogPresenter).showForListeners(fragmentRule.getActivity());
    }

    @Test
    public void shouldPublishTrackingEventOnFacebookCloseButtonClick() {
        when(adapter.getItem(0)).thenReturn(forFacebookListenerInvites());

        presenter.onListenerInvitesDismiss(0);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(FacebookInvitesEvent.class);
    }

    @Test
    public void shouldNotOpenFacebookInvitesDialogOnFacebookInvitesCloseButtonClick() {
        when(adapter.getItem(0)).thenReturn(forFacebookListenerInvites());
        presenter.onCreate(fragmentRule.getFragment(), null);

        presenter.onListenerInvitesDismiss(0);

        verify(facebookInvitesDialogPresenter, never()).showForListeners(fragmentRule.getActivity());
    }

    @Test
    public void shouldNotDoAnythingWhenClickingOnFacebookInvitesNotification() {
        when(adapter.getItem(0)).thenReturn(forFacebookListenerInvites());

        presenter.onItemClicked(view, 0);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).isEmpty();
        verify(facebookInvitesDialogPresenter, never()).showForListeners(fragmentRule.getActivity());
    }

    @Test
    public void onStationOnboardingItemClosedDisableOnboarding() {
        presenter.onStationOnboardingItemClosed(0);

        verify(stationsOperations).disableOnboardingStreamItem();
    }

    @Test
    public void onUpsellItemDismissedUpsellsGetDisabled() {
        presenter.onUpsellItemDismissed(0);

        verify(streamOperations).disableUpsell();
    }

    @Test
    public void onUpsellItemClickedOpensUpgradeScreen() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onUpsellItemClicked(context(), 0);

        verify(navigator).openUpgrade(context());
    }

    @Test
    public void onUpsellItemClickedSendsUpsellTrackingEvent() {
        UpgradeFunnelEvent expectedEvent = UpgradeFunnelEvent.forStreamClick();

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onUpsellItemClicked(context(), 0);

        UpgradeFunnelEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING, UpgradeFunnelEvent.class);
        assertThat(trackingEvent.getKind()).isEqualTo(expectedEvent.getKind());
    }

    @Test
    public void onRefreshableOverlayClickedUpdatesStreamAgain() {
        when(streamOperations.initialStreamItems())
                .thenReturn(Observable.just(Collections.emptyList()));
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onNewItemsIndicatorClicked();

        verify(streamOperations, times(2)).initialStreamItems();
    }

    @Test
    public void onStreamRefreshNewItemsSinceDate() {
        when(streamOperations.newItemsSince(123L)).thenReturn(Observable.just(5));
        when(streamOperations.getFirstItemTimestamp(anyListOf(StreamItem.class)))
                .thenReturn(Optional.of(DATE));

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        eventBus.publish(EventQueue.STREAM, StreamEvent.fromStreamRefresh());

        verify(newItemsIndicator).update(5);
    }

    @Test
    public void onStreamRefreshUpdatesOnlyWhenThereAreVisibleItems() {
        when(streamOperations.newItemsSince(123L)).thenReturn(Observable.just(5));
        when(streamOperations.getFirstItemTimestamp(anyListOf(StreamItem.class))).thenReturn(null);

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        eventBus.publish(EventQueue.STREAM, StreamEvent.fromStreamRefresh());

        verify(newItemsIndicator, never()).update(5);
    }

    @Test
    public void shouldRefreshOnCreate() {
        when(streamOperations.updatedTimelineItemsForStart()).thenReturn(Observable.just(Collections.emptyList()));
        when(streamOperations.getFirstItemTimestamp(anyListOf(StreamItem.class))).thenReturn(Optional.of(DATE));
        when(streamOperations.newItemsSince(123L)).thenReturn(Observable.just(5));

        presenter.onCreate(fragmentRule.getFragment(), null);

        verify(newItemsIndicator).update(5);
    }

    @Test
    public void shouldNotUpdateIndicatorWhenUpdatedItemsForStartIsEmpty() {
        when(streamOperations.updatedTimelineItemsForStart()).thenReturn(Observable.empty());
        when(streamOperations.getFirstItemTimestamp(anyListOf(StreamItem.class))).thenReturn(Optional.of(DATE));
        when(streamOperations.newItemsSince(123L)).thenReturn(Observable.just(5));

        presenter.onCreate(fragmentRule.getFragment(), null);

        verify(newItemsIndicator, never()).update(5);
    }

    @Test
    public void shouldResetOverlayOnRefreshBinding() {
        when(streamOperations.updatedStreamItems()).thenReturn(Observable.empty());

        presenter.onRefreshBinding();

        verify(newItemsIndicator).hideAndReset();
    }

    @Test
    public void shouldSetOverlayViewOnViewCreated() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        verify(newItemsIndicator).setTextView(or(isNull(), any(TextView.class)));
    }

    @Test
    public void shouldSetOnStreamAdsControllerOnViewCreated() {
        presenter.onCreate(fragmentRule.getFragment(), null);

        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        verify(streamAdsController).onViewCreated(any(RecyclerView.class), eq(adapter));
    }

    @Test
    public void shouldClearStreamAdControllerOnViewDestroy() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onDestroyView(fragmentRule.getFragment());

        verify(streamAdsController).onDestroyView();
    }

    @Test
    public void shouldForwardStreamDestroyToStreamAdsController() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onDestroy(fragmentRule.getFragment());

        verify(streamAdsController).onDestroy();
    }

    @Test
    public void shouldForwardStreamDestroyToVideoSurfaceProvider() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onDestroy(fragmentRule.getFragment());

        verify(videoSurfaceProvider).onDestroy(Origin.STREAM);
    }

    @Test
    public void shouldForwardOnFocusGainToStreamAdsController() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onFocusChange(true);

        verify(streamAdsController).onFocusGain();
    }

    @Test
    public void shouldForwardOnFocusLossToStreamAdsController() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onFocusChange(false);

        verify(streamAdsController).onFocusLoss(true);
    }

    @Test
    public void shouldCallOnFocusChangeInStreamAdsControllerWhenOnResumeIsCalled() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onResume(fragmentRule.getFragment());

        verify(streamAdsController).onResume(false);
    }

    @Test
    public void shouldCallOnPauseInStreamAdsControllerWhenOnPauseIsCalled() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onPause(fragmentRule.getFragment());

        verify(streamAdsController).onPause(fragmentRule.getFragment());
    }

    @Test
    public void shouldForwardOrientationChangeToVideoSurfaceProvider() {
        final Fragment fragment = mock(Fragment.class);
        final FragmentActivity activity = mock(FragmentActivity.class);
        when(fragment.getActivity()).thenReturn(activity);
        when(activity.isChangingConfigurations()).thenReturn(true);

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        presenter.onDestroy(fragment);

        verify(videoSurfaceProvider).onConfigurationChange(Origin.STREAM);
    }

    @Test
    public void shouldCreatedStreamDepthPublisherOnViewCreated() {
        presenter.onCreate(fragmentRule.getFragment(), null);

        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        final StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) presenter.getRecyclerView().getLayoutManager();
        verify(streamDepthPublisherFactory).create(layoutManager, false);
    }

    @Test
    public void shouldClearScrollDepthTrackingControllerOnViewDestroy() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onDestroyView(fragmentRule.getFragment());

        verify(streamDepthPublisher).unsubscribe();
    }

    @Test
    public void shouldForceRefreshOnFollowAndUnfollow() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        followSubject.onNext(Urn.forUser(123L));
        unfollowSubject.onNext(Urn.forUser(456L));

        verify(swipeRefreshAttacher, times(2)).forceRefresh();
    }

    @Test
    public void shouldForwardOpenWhyAdsCallToPresenter() {
        when(adapter.getItem(0)).thenReturn(forFacebookListenerInvites());
        presenter.onCreate(fragmentRule.getFragment(), null);

        presenter.onWhyAdsClicked(view.getContext());

        verify(whyAdsDialogPresenter).show(view.getContext());
    }

    @Test
    public void shouldNavigateAndEmitTrackingEventForAppInstallClickthroughs() {
        final AppInstallAd appInstall = AppInstallAd.create(AdFixtures.getApiAppInstall(), 42424242);

        when(adapter.getItem(0)).thenReturn(forFacebookListenerInvites());
        presenter.onCreate(fragmentRule.getFragment(), null);

        presenter.onAdItemClicked(view.getContext(), appInstall);

        verify(navigator).openAdClickthrough(view.getContext(), Uri.parse(appInstall.getClickThroughUrl()));
        final UIEvent trackingEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(trackingEvent.kind()).isEqualTo(UIEvent.Kind.AD_CLICKTHROUGH);
    }

    @Test
    public void shouldNavigateAndEmitTrackingEventForVideoAdClickthroughs() {
        final VideoAd videoAd = AdFixtures.getInlayVideoAd(32L);

        when(adapter.getItem(0)).thenReturn(forFacebookListenerInvites());
        presenter.onCreate(fragmentRule.getFragment(), null);

        presenter.onAdItemClicked(view.getContext(), videoAd);

        verify(navigator).openAdClickthrough(view.getContext(), Uri.parse(videoAd.getClickThroughUrl()));
        final UIEvent trackingEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(trackingEvent.kind()).isEqualTo(UIEvent.Kind.AD_CLICKTHROUGH);
    }

    @Test
    public void resumesImageLoadingOnViewDestroy() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onDestroyView(fragmentRule.getFragment());

        verify(imagePauseOnScrollListener).resume();
    }

    @Test
    public void shouldEndMeasuringLoginPerformanceWhenStreamIsHome() {

        final TrackItem trackItem = expectedTrackForListItem(Urn.forTrack(123L));
        TrackStreamItem normalTrackStreamItem = TrackStreamItem.create(trackItem, CREATED_AT, Optional.absent());
        List<StreamItem> items = Collections.singletonList(normalTrackStreamItem);

        when(streamOperations.initialStreamItems()).thenReturn(Observable.just(items));
        when(defaultHomeScreenConfiguration.isStreamHome()).thenReturn(true);

        presenter.onCreate(fragmentRule.getFragment(), null);

        verify(performanceMetricsEngine).endMeasuring(performanceMetricArgumentCaptor.capture());

        Assertions.assertThat(performanceMetricArgumentCaptor.getValue())
                  .hasMetricType(MetricType.LOGIN)
                  .containsMetricParam(MetricKey.HOME_SCREEN, Screen.STREAM.get());
    }

    @Test
    public void shouldNotEndMeasuringLoginPerformanceWhenStreamIsNotHome() {

        final TrackItem trackItem = expectedTrackForListItem(Urn.forTrack(123L));
        TrackStreamItem normalTrackStreamItem = TrackStreamItem.create(trackItem, CREATED_AT, Optional.absent());
        List<StreamItem> items = Collections.singletonList(normalTrackStreamItem);

        when(streamOperations.initialStreamItems()).thenReturn(Observable.just(items));
        when(defaultHomeScreenConfiguration.isStreamHome()).thenReturn(false);

        presenter.onCreate(fragmentRule.getFragment(), null);

        verify(performanceMetricsEngine, never()).endMeasuring(any(PerformanceMetric.class));
    }

    @Test
    public void shouldSetTextureViewForVideoAdUsingVideoSurfaceProvider() {
        final VideoAd videoAd = AdFixtures.getInlayVideoAd(32L);

        presenter.onVideoTextureBind(textureView, videoAd);

        verify(videoSurfaceProvider).setTextureView(videoAd.getUuid(), Origin.STREAM, textureView);
    }

    @Test
    public void shouldSetTextureViewForVideoAdIsntSetInVideoSurfaceProviderIfVideoInFullscreen() {
        when(streamAdsController.isInFullscreen()).thenReturn(true);
        final VideoAd videoAd = AdFixtures.getInlayVideoAd(32L);

        presenter.onVideoTextureBind(textureView, videoAd);

        verify(videoSurfaceProvider, never()).setTextureView(videoAd.getUuid(), Origin.STREAM, textureView);
    }

    @Test
    public void shouldSetFullscreenEnabledAndOpenFullscreenVideoAdOnVideoFullscreenClicked() {
        final VideoAd videoAd = AdFixtures.getInlayVideoAd(32L);

        presenter.onVideoFullscreenClicked(context(), videoAd);

        verify(streamAdsController).setFullscreenEnabled();
        verify(navigator).openFullscreenVideoAd(context(), videoAd.getAdUrn());
    }
}
