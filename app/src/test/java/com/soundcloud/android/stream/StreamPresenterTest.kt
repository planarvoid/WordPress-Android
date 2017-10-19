package com.soundcloud.android.stream

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.TextureView
import android.view.View
import android.widget.TextView
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.R
import com.soundcloud.android.ads.AdFixtures
import com.soundcloud.android.ads.AppInstallAd
import com.soundcloud.android.ads.StreamAdsController
import com.soundcloud.android.ads.WhyAdsDialogPresenter
import com.soundcloud.android.associations.FollowingOperations
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.events.StreamEvent
import com.soundcloud.android.events.TrackingEvent
import com.soundcloud.android.events.UIEvent
import com.soundcloud.android.facebookinvites.FacebookInvitesDialogPresenter
import com.soundcloud.android.helpers.NavigationTargetMatcher.matchesNavigationTarget
import com.soundcloud.android.image.ImagePauseOnScrollListener
import com.soundcloud.android.main.Screen
import com.soundcloud.android.model.Urn
import com.soundcloud.android.navigation.NavigationExecutor
import com.soundcloud.android.navigation.NavigationTarget
import com.soundcloud.android.navigation.Navigator
import com.soundcloud.android.playback.PlayableWithReposter
import com.soundcloud.android.playback.VideoSurfaceProvider
import com.soundcloud.android.playback.VideoSurfaceProvider.Origin
import com.soundcloud.android.stream.perf.StreamMeasurements
import com.soundcloud.android.stream.perf.StreamMeasurementsFactory
import com.soundcloud.android.testsupport.AndroidUnitTest
import com.soundcloud.android.testsupport.FragmentRule
import com.soundcloud.android.testsupport.RefreshableFragmentRule
import com.soundcloud.android.testsupport.TestPager
import com.soundcloud.android.testsupport.fixtures.ModelFixtures
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures.expectedLikedPlaylistForPlaylistsScreen
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures.expectedPromotedTrack
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures.expectedTrackForListItem
import com.soundcloud.android.tracks.UpdatePlayableAdapterObserver
import com.soundcloud.android.utils.DateProvider
import com.soundcloud.android.view.NewItemsIndicator
import com.soundcloud.android.view.adapters.MixedItemClickListener
import com.soundcloud.java.optional.Optional
import com.soundcloud.rx.eventbus.TestEventBusV2
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.AdditionalMatchers.or
import org.mockito.ArgumentMatchers.argThat
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mock
import org.mockito.Mockito.doCallRealMethod
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Spy
import rx.Observer
import java.util.Arrays
import java.util.Date

class StreamPresenterTest : AndroidUnitTest() {

    @JvmField @Rule val fragmentRule: FragmentRule = RefreshableFragmentRule(R.layout.default_recyclerview_with_refresh)

    @Mock private lateinit var streamOperations: StreamOperations
    @Mock private lateinit var adapter: StreamAdapter
    @Mock private lateinit var imagePauseOnScrollListener: ImagePauseOnScrollListener
    @Mock private lateinit var streamAdsController: StreamAdsController
    @Mock private lateinit var streamDepthPublisherFactory: StreamDepthPublisher.Factory
    @Mock private lateinit var streamDepthPublisher: StreamDepthPublisher
    @Mock private lateinit var dateProvider: DateProvider
    @Mock private lateinit var itemObserver: Observer<Iterable<StreamItem>>
    @Mock private lateinit var itemClickListenerFactory: MixedItemClickListener.Factory
    @Mock private lateinit var itemClickListener: MixedItemClickListener
    @Mock private lateinit var navigationExecutor: NavigationExecutor
    @Mock private lateinit var navigator: Navigator
    @Mock private lateinit var facebookInvitesDialogPresenter: FacebookInvitesDialogPresenter
    @Mock private lateinit var view: View
    @Mock private lateinit var newItemsIndicator: NewItemsIndicator
    @Mock private lateinit var followingOperations: FollowingOperations
    @Mock private lateinit var updatePlayableAdapterObserverFactory: UpdatePlayableAdapterObserver.Factory
    @Mock private lateinit var whyAdsDialogPresenter: WhyAdsDialogPresenter
    @Mock private lateinit var videoSurfaceProvider: VideoSurfaceProvider
    @Mock private lateinit var textureView: TextureView
    @Mock private lateinit var streamMeasurementsFactory: StreamMeasurementsFactory
    @Mock private lateinit var streamMeasurements: StreamMeasurements

    @Spy private lateinit var swipeRefreshAttacher: StreamSwipeRefreshAttacher

    private lateinit var updatePlayableAdapterObserver: UpdatePlayableAdapterObserver
    private val eventBus = TestEventBusV2()
    private val followSubject = PublishSubject.create<Urn>()
    private val unfollowSubject = PublishSubject.create<Urn>()
    private val DATE = Date(123L)
    private lateinit var presenter: StreamPresenter

    @Before
    @Throws(Exception::class)
    fun setUp() {

        updatePlayableAdapterObserver = spy(UpdatePlayableAdapterObserver(adapter))
        whenever(updatePlayableAdapterObserverFactory.create(adapter)).thenReturn(updatePlayableAdapterObserver)
        whenever(itemClickListenerFactory.create(Screen.STREAM, null)).thenReturn(itemClickListener)
        whenever(streamMeasurementsFactory.create()).thenReturn(streamMeasurements)
        doNothing().whenever(swipeRefreshAttacher).forceRefresh()

        presenter = StreamPresenter(
                streamOperations,
                adapter,
                imagePauseOnScrollListener,
                streamAdsController,
                streamDepthPublisherFactory,
                eventBus,
                itemClickListenerFactory,
                swipeRefreshAttacher,
                facebookInvitesDialogPresenter,
                navigationExecutor,
                navigator,
                newItemsIndicator,
                followingOperations,
                whyAdsDialogPresenter,
                videoSurfaceProvider,
                updatePlayableAdapterObserverFactory,
                streamMeasurementsFactory)

        whenever(streamOperations.initialStreamItems()).thenReturn(Single.just(emptyList()))
        whenever(streamOperations.updatedTimelineItemsForStart()).thenReturn(Maybe.empty())
        whenever(streamOperations.pagingFunction()).thenReturn(TestPager.singlePageFunction<List<StreamItem>>())
        whenever(dateProvider.currentTime).thenReturn(100L)
        whenever(followingOperations.onUserFollowed()).thenReturn(followSubject)
        whenever(followingOperations.onUserUnfollowed()).thenReturn(unfollowSubject)
        whenever(streamDepthPublisherFactory.create(any(), any())).thenReturn(streamDepthPublisher)
        whenever(streamAdsController.isInFullscreen).thenReturn(false)
    }

    @Test
    fun canLoadStreamItems() {
        val promotedTrackItem = expectedPromotedTrack()
        val promotedTrackStreamItem = TrackStreamItem.create(promotedTrackItem, CREATED_AT, Optional.absent())
        val trackItem = expectedTrackForListItem(Urn.forTrack(123L))
        val normalTrackStreamItem = TrackStreamItem.create(trackItem, CREATED_AT, Optional.absent())
        val playlistItem = expectedLikedPlaylistForPlaylistsScreen()
        val playlistStreamItem = PlaylistStreamItem.create(playlistItem, CREATED_AT, Optional.absent())
        val items = Arrays.asList<StreamItem>(promotedTrackStreamItem,
                                              normalTrackStreamItem,
                                              playlistStreamItem)
        whenever(streamOperations.initialStreamItems()).thenReturn(Single.just(items))

        val binding = presenter.onBuildBinding(null)
        binding.connect()
        binding.items().subscribe(itemObserver)

        verify(itemObserver).onNext(items)
        verify(streamOperations, never()).publishPromotedImpression(items)
    }

    @Test
    fun onBuildBindingPublishesPromotedImpressionWhenStreamInFocus() {
        val promotedTrackItem = expectedPromotedTrack()
        val promotedTrackStreamItem = TrackStreamItem.create(promotedTrackItem, CREATED_AT, Optional.absent())
        val trackItem = expectedTrackForListItem(Urn.forTrack(123L))
        val normalTrackStreamItem = TrackStreamItem.create(trackItem, CREATED_AT, Optional.absent())
        val playlistItem = expectedLikedPlaylistForPlaylistsScreen()
        val playlistStreamItem = PlaylistStreamItem.create(playlistItem, CREATED_AT, Optional.absent())
        val items = Arrays.asList<StreamItem>(promotedTrackStreamItem,
                                              normalTrackStreamItem,
                                              playlistStreamItem)
        whenever(streamOperations.initialStreamItems()).thenReturn(Single.just(items))
        presenter.onFocusChange(true)

        val binding = presenter.onBuildBinding(null)
        binding.connect()
        binding.items().subscribe(itemObserver)

        verify(itemObserver).onNext(items)
        verify(streamOperations).publishPromotedImpression(items)
    }

    @Test
    fun canRefreshStreamItems() {
        val trackItem = expectedTrackForListItem(Urn.forTrack(123L))
        val streamItem = TrackStreamItem.create(trackItem, CREATED_AT, Optional.absent())
        val items = listOf<StreamItem>(streamItem)
        whenever(streamOperations.updatedStreamItems()).thenReturn(Single.just(items))

        val binding = presenter.onRefreshBinding()
        binding.connect()
        binding.items().subscribe(itemObserver)

        verify(itemObserver).onNext(items)
        verify(streamOperations, never()).publishPromotedImpression(items)
    }

    @Test
    fun onRefreshBindingPublishesPromotedImpressionWhenStreamInFocus() {
        val trackItem = expectedTrackForListItem(Urn.forTrack(123L))
        val streamItem = TrackStreamItem.create(trackItem, CREATED_AT, Optional.absent())
        val items = listOf<StreamItem>(streamItem)
        whenever(streamOperations.updatedStreamItems()).thenReturn(Single.just(items))
        presenter.onFocusChange(true)

        val binding = presenter.onRefreshBinding()
        binding.connect()
        binding.items().subscribe(itemObserver)

        verify(itemObserver).onNext(items)
        verify(streamOperations).publishPromotedImpression(items)
    }

    @Test
    fun forwardsTrackClicksToClickListener() {
        val clickedTrack = ModelFixtures.trackItem()
        val streamTracks = Single.just(Arrays.asList(PlayableWithReposter.from(clickedTrack.urn), PlayableWithReposter.from(Urn.forTrack(634L))))

        whenever(adapter.getItem(0)).thenReturn(TrackStreamItem.create(clickedTrack, clickedTrack.createdAt, Optional.absent()))
        whenever(streamOperations.urnsForPlayback()).thenReturn(streamTracks)

        presenter.onItemClicked(view, 0)

        verify<MixedItemClickListener>(itemClickListener).legacyOnPostClick(any(),
                                                                            eq(view),
                                                                            eq(0),
                                                                            eq(clickedTrack))
    }

    @Test
    fun tracksPromotedTrackItemClick() {
        val clickedTrack = expectedPromotedTrack()

        whenever(streamOperations.newItemsSince(123L)).thenReturn(io.reactivex.Observable.just(5))
        whenever(streamOperations.getFirstItemTimestamp(any())).thenReturn(Optional.absent())

        presenter.onCreate(fragmentRule.fragment, null)
        presenter.onViewCreated(fragmentRule.fragment, fragmentRule.view, null)

        eventBus.publish(EventQueue.STREAM, StreamEvent.fromStreamRefresh())

        verify<NewItemsIndicator>(newItemsIndicator, never()).update(5)
    }

    @Test
    fun shouldRefreshOnCreate() {
        whenever(streamOperations.updatedTimelineItemsForStart()).thenReturn(Maybe.just(emptyList()))
        whenever(streamOperations.getFirstItemTimestamp(any())).thenReturn(Optional.of(DATE))
        whenever(streamOperations.newItemsSince(123L)).thenReturn(io.reactivex.Observable.just(5))

        presenter.onCreate(fragmentRule.fragment, null)

        verify<NewItemsIndicator>(newItemsIndicator).update(5)
    }

    @Test
    fun shouldNotUpdateIndicatorWhenUpdatedItemsForStartIsEmpty() {
        whenever(streamOperations.updatedTimelineItemsForStart()).thenReturn(Maybe.empty())
        whenever(streamOperations.getFirstItemTimestamp(any())).thenReturn(Optional.of(DATE))
        whenever(streamOperations.newItemsSince(123L)).thenReturn(io.reactivex.Observable.just(5))

        presenter.onCreate(fragmentRule.fragment, null)

        verify<NewItemsIndicator>(newItemsIndicator, never()).update(5)
    }

    @Test
    fun shouldResetOverlayOnRefreshBinding() {
        whenever(streamOperations.updatedStreamItems()).thenReturn(Single.just(emptyList()))

        presenter.onRefreshBinding()

        verify<NewItemsIndicator>(newItemsIndicator).hideAndReset()
    }

    @Test
    fun shouldSetOverlayViewOnViewCreated() {
        presenter.onCreate(fragmentRule.fragment, null)
        presenter.onViewCreated(fragmentRule.fragment, fragmentRule.view, null)

        verify<NewItemsIndicator>(newItemsIndicator).setTextView(or(isNull<TextView>(), any()))
    }

    @Test
    fun shouldSetOnStreamAdsControllerOnViewCreated() {
        presenter.onCreate(fragmentRule.fragment, null)

        presenter.onViewCreated(fragmentRule.fragment, fragmentRule.view, null)

        verify<StreamAdsController>(streamAdsController).onViewCreated(any(), eq(adapter))
    }

    @Test
    fun onViewCreatedShouldHandlePromotedImpressionIfStreamInFocus() {
        presenter.onCreate(fragmentRule.fragment, null)
        whenever(adapter.items).thenReturn(emptyList())
        presenter.onFocusChange(true)

        presenter.onViewCreated(fragmentRule.fragment, fragmentRule.view, null)

        verify<StreamOperations>(streamOperations, times(2)).publishPromotedImpression(emptyList())
    }

    @Test
    fun onViewCreatedShouldNotHandlePromotedImpressionIfStreamIsNotInFocus() {
        presenter.onCreate(fragmentRule.fragment, null)
        whenever(adapter.items).thenReturn(emptyList())
        presenter.onFocusChange(false)

        presenter.onViewCreated(fragmentRule.fragment, fragmentRule.view, null)

        verify<StreamOperations>(streamOperations, never()).publishPromotedImpression(emptyList())
    }

    @Test
    fun shouldClearStreamAdControllerOnViewDestroy() {
        presenter.onCreate(fragmentRule.fragment, null)
        presenter.onViewCreated(fragmentRule.fragment, fragmentRule.view, null)

        presenter.onDestroyView(fragmentRule.fragment)

        verify<StreamAdsController>(streamAdsController).onDestroyView()
    }

    @Test
    fun shouldForwardStreamDestroyToStreamAdsController() {
        presenter.onCreate(fragmentRule.fragment, null)
        presenter.onViewCreated(fragmentRule.fragment, fragmentRule.view, null)

        presenter.onDestroy(fragmentRule.fragment)

        verify<StreamAdsController>(streamAdsController).onDestroy()
    }

    @Test
    fun shouldForwardStreamDestroyToVideoSurfaceProvider() {
        presenter.onCreate(fragmentRule.fragment, null)
        presenter.onViewCreated(fragmentRule.fragment, fragmentRule.view, null)

        presenter.onDestroy(fragmentRule.fragment)

        verify<VideoSurfaceProvider>(videoSurfaceProvider).onDestroy(Origin.STREAM)
    }

    @Test
    fun shouldForwardOnFocusGainToStreamAdsController() {
        presenter.onCreate(fragmentRule.fragment, null)
        presenter.onViewCreated(fragmentRule.fragment, fragmentRule.view, null)

        presenter.onFocusChange(true)

        verify<StreamAdsController>(streamAdsController).onFocusGain()
    }

    @Test
    fun shouldForwardOnFocusLossToStreamAdsController() {
        presenter.onCreate(fragmentRule.fragment, null)
        presenter.onViewCreated(fragmentRule.fragment, fragmentRule.view, null)

        presenter.onFocusChange(false)

        verify<StreamAdsController>(streamAdsController).onFocusLoss(true)
    }

    @Test
    fun shouldHandlePromotedImpressionIfStreamInFocus() {
        presenter.onCreate(fragmentRule.fragment, null)

        whenever(adapter.items).thenReturn(emptyList())

        presenter.onFocusChange(true)

        verify<StreamOperations>(streamOperations).publishPromotedImpression(emptyList())
    }

    @Test
    fun shouldNotHandlePromotedImpressionIfStreamIsNotInFocus() {
        presenter.onCreate(fragmentRule.fragment, null)

        whenever(adapter.items).thenReturn(emptyList())

        presenter.onFocusChange(false)

        verify<StreamOperations>(streamOperations, never()).publishPromotedImpression(emptyList())
    }

    @Test
    fun shouldCallOnFocusChangeInStreamAdsControllerWhenOnResumeIsCalled() {
        presenter.onCreate(fragmentRule.fragment, null)
        presenter.onViewCreated(fragmentRule.fragment, fragmentRule.view, null)

        presenter.onResume(fragmentRule.fragment)

        verify<StreamAdsController>(streamAdsController).onResume(false)
    }

    @Test
    fun shouldCallOnPauseInStreamAdsControllerWhenOnPauseIsCalled() {
        presenter.onCreate(fragmentRule.fragment, null)
        presenter.onViewCreated(fragmentRule.fragment, fragmentRule.view, null)

        presenter.onPause(fragmentRule.fragment)

        verify<StreamAdsController>(streamAdsController).onPause(fragmentRule.fragment)
    }

    @Test
    fun shouldForwardOrientationChangeToVideoSurfaceProvider() {
        val fragment = mock<Fragment>()
        val activity = mock<FragmentActivity>()
        whenever(fragment.activity).thenReturn(activity)
        whenever(activity.isChangingConfigurations).thenReturn(true)

        presenter.onCreate(fragmentRule.fragment, null)
        presenter.onViewCreated(fragmentRule.fragment, fragmentRule.view, null)
        presenter.onDestroy(fragment)

        verify<VideoSurfaceProvider>(videoSurfaceProvider).onConfigurationChange(Origin.STREAM)
    }

    @Test
    fun shouldCreatedStreamDepthPublisherOnViewCreated() {
        presenter.onCreate(fragmentRule.fragment, null)

        presenter.onViewCreated(fragmentRule.fragment, fragmentRule.view, null)

        val layoutManager = presenter.recyclerView.layoutManager as StaggeredGridLayoutManager
        verify<StreamDepthPublisher.Factory>(streamDepthPublisherFactory).create(layoutManager, false)
    }

    @Test
    fun shouldClearScrollDepthTrackingControllerOnViewDestroy() {
        presenter.onCreate(fragmentRule.fragment, null)
        presenter.onViewCreated(fragmentRule.fragment, fragmentRule.view, null)

        presenter.onDestroyView(fragmentRule.fragment)

        verify<StreamDepthPublisher>(streamDepthPublisher).unsubscribe()
    }

    @Test
    fun shouldForceRefreshOnFollowAndUnfollow() {
        presenter.onCreate(fragmentRule.fragment, null)
        presenter.onViewCreated(fragmentRule.fragment, fragmentRule.view, null)

        followSubject.onNext(Urn.forUser(123L))
        unfollowSubject.onNext(Urn.forUser(456L))

        verify<StreamSwipeRefreshAttacher>(swipeRefreshAttacher, times(2)).forceRefresh()
    }

    @Test
    fun shouldForwardOpenWhyAdsCallToPresenter() {
        whenever(adapter.getItem(0)).thenReturn(StreamItem.FacebookListenerInvites())
        presenter.onCreate(fragmentRule.fragment, null)

        val context = mock<Context>()

        presenter.onWhyAdsClicked(context)

        verify<WhyAdsDialogPresenter>(whyAdsDialogPresenter).show(context)
    }

    @Test
    fun shouldNavigateAndEmitTrackingEventForAppInstallClickthroughs() {
        val appInstall = AppInstallAd.create(AdFixtures.getApiAppInstall(), 42424242)

        whenever(adapter.getItem(0)).thenReturn(StreamItem.FacebookListenerInvites())
        presenter.onCreate(fragmentRule.fragment, null)

        presenter.onAdItemClicked(appInstall)

        verify<Navigator>(navigator).navigateTo(argThat(matchesNavigationTarget(NavigationTarget.forAdClickthrough(appInstall.clickThroughUrl()))))
        val trackingEvent = eventBus.lastEventOn<TrackingEvent>(EventQueue.TRACKING) as UIEvent
        assertThat(trackingEvent.kind()).isEqualTo(UIEvent.Kind.AD_CLICKTHROUGH)
    }

    @Test
    fun shouldNavigateAndEmitTrackingEventForVideoAdClickthroughs() {
        val videoAd = AdFixtures.getInlayVideoAd(32L)

        whenever(adapter.getItem(0)).thenReturn(StreamItem.FacebookListenerInvites())
        presenter.onCreate(fragmentRule.fragment, null)

        presenter.onAdItemClicked(videoAd)

        verify<Navigator>(navigator).navigateTo(argThat(matchesNavigationTarget(NavigationTarget.forAdClickthrough(videoAd.clickThroughUrl()))))
        val trackingEvent = eventBus.lastEventOn<TrackingEvent>(EventQueue.TRACKING) as UIEvent
        assertThat(trackingEvent.kind()).isEqualTo(UIEvent.Kind.AD_CLICKTHROUGH)
    }

    @Test
    fun resumesImageLoadingOnViewDestroy() {
        presenter.onCreate(fragmentRule.fragment, null)
        presenter.onViewCreated(fragmentRule.fragment, fragmentRule.view, null)

        presenter.onDestroyView(fragmentRule.fragment)

        verify<ImagePauseOnScrollListener>(imagePauseOnScrollListener).resume()
    }

    @Test
    fun shouldEndMeasuringLoginPerformanceWhenStreamIsHome() {

        val trackItem = expectedTrackForListItem(Urn.forTrack(123L))
        val normalTrackStreamItem = TrackStreamItem.create(trackItem, CREATED_AT, Optional.absent())
        val items = listOf<StreamItem>(normalTrackStreamItem)

        whenever(streamOperations.initialStreamItems()).thenReturn(Single.just(items))

        presenter.onCreate(fragmentRule.fragment, null)

        verify<StreamMeasurements>(streamMeasurements).endLoading()
    }

    @Test
    fun shouldInvokeStreamMeasurementsOnRefresh() {

        doCallRealMethod().whenever(swipeRefreshAttacher).forceRefresh()

        val trackItem = expectedTrackForListItem(Urn.forTrack(123L))
        val streamItem = TrackStreamItem.create(trackItem, CREATED_AT, Optional.absent())
        whenever(streamOperations.updatedStreamItems()).thenReturn(Single.just(
                listOf<StreamItem>(streamItem)
        ))

        presenter.onCreate(fragmentRule.fragment, null)
        presenter.onViewCreated(fragmentRule.fragment, fragmentRule.view, null)

        swipeRefreshAttacher.forceRefresh()

        val inOrder = inOrder(streamMeasurements)
        inOrder.verify<StreamMeasurements>(streamMeasurements).startRefreshing()
        inOrder.verify<StreamMeasurements>(streamMeasurements).endRefreshing()
    }

    @Test
    fun shouldSetTextureViewForVideoAdUsingVideoSurfaceProvider() {
        val videoAd = AdFixtures.getInlayVideoAd(32L)

        presenter.onVideoTextureBind(textureView, view, videoAd)

        verify<VideoSurfaceProvider>(videoSurfaceProvider).setTextureView(videoAd.uuid(), Origin.STREAM, textureView, view)
    }

    @Test
    fun shouldSetTextureViewForVideoAdIsntSetInVideoSurfaceProviderIfVideoInFullscreen() {
        whenever(streamAdsController.isInFullscreen).thenReturn(true)
        val videoAd = AdFixtures.getInlayVideoAd(32L)

        presenter.onVideoTextureBind(textureView, view, videoAd)

        verify<VideoSurfaceProvider>(videoSurfaceProvider, never()).setTextureView(videoAd.uuid(), Origin.STREAM, textureView, view)
    }

    @Test
    fun shouldSetFullscreenEnabledAndOpenFullscreenVideoAdOnVideoFullscreenClicked() {
        val videoAd = AdFixtures.getInlayVideoAd(32L)

        presenter.onCreate(fragmentRule.fragment, null)
        presenter.onVideoFullscreenClicked(videoAd)

        verify<StreamAdsController>(streamAdsController).setFullscreenEnabled()
        verify<Navigator>(navigator).navigateTo(argThat(matchesNavigationTarget(NavigationTarget.forFullscreenVideoAd(videoAd.adUrn()))))
    }

    companion object {

        private val CREATED_AT = Date()
    }
}
