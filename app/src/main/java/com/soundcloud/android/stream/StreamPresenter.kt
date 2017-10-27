package com.soundcloud.android.stream

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.TextureView
import android.view.View
import com.soundcloud.android.Actions
import com.soundcloud.android.R
import com.soundcloud.android.ads.AdData
import com.soundcloud.android.ads.AdItemResult
import com.soundcloud.android.ads.AppInstallAd
import com.soundcloud.android.ads.StreamAdsController
import com.soundcloud.android.ads.VideoAd
import com.soundcloud.android.ads.WhyAdsDialogPresenter
import com.soundcloud.android.associations.FollowingOperations
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.events.FacebookInvitesEvent
import com.soundcloud.android.events.FacebookInvitesEvent.forCreatorClick
import com.soundcloud.android.events.FacebookInvitesEvent.forCreatorDismiss
import com.soundcloud.android.events.FacebookInvitesEvent.forListenerClick
import com.soundcloud.android.events.FacebookInvitesEvent.forListenerDismiss
import com.soundcloud.android.events.FacebookInvitesEvent.forListenerShown
import com.soundcloud.android.events.PromotedTrackingEvent
import com.soundcloud.android.events.TrackingEvent
import com.soundcloud.android.events.UIEvent
import com.soundcloud.android.events.UpgradeFunnelEvent
import com.soundcloud.android.facebookinvites.FacebookInvitesDialogPresenter
import com.soundcloud.android.facebookinvites.FacebookLoadingResult
import com.soundcloud.android.image.ImagePauseOnScrollListener
import com.soundcloud.android.main.Screen
import com.soundcloud.android.model.Urn
import com.soundcloud.android.navigation.NavigationExecutor
import com.soundcloud.android.navigation.NavigationTarget
import com.soundcloud.android.navigation.Navigator
import com.soundcloud.android.payments.UpsellContext
import com.soundcloud.android.playback.PlayableWithReposter
import com.soundcloud.android.playback.TrackSourceInfo
import com.soundcloud.android.playback.VideoSurfaceProvider
import com.soundcloud.android.playback.VideoSurfaceProvider.Origin
import com.soundcloud.android.presentation.CollectionBinding
import com.soundcloud.android.presentation.PlayableItem
import com.soundcloud.android.presentation.RecyclerViewPresenter
import com.soundcloud.android.rx.RxJava
import com.soundcloud.android.rx.observers.DefaultObserver
import com.soundcloud.android.rx.observers.LambdaObserver
import com.soundcloud.android.rx.observers.LambdaSubscriber.onNext
import com.soundcloud.android.stream.StreamItem.FacebookListenerInvites
import com.soundcloud.android.stream.perf.StreamMeasurements
import com.soundcloud.android.stream.perf.StreamMeasurementsFactory
import com.soundcloud.android.sync.timeline.TimelinePresenter
import com.soundcloud.android.tracks.UpdatePlayableAdapterObserver
import com.soundcloud.android.upsell.UpsellLoadingResult
import com.soundcloud.android.utils.ErrorUtils
import com.soundcloud.android.view.EmptyView
import com.soundcloud.android.view.NewItemsIndicator
import com.soundcloud.android.view.adapters.LikeEntityListObserver
import com.soundcloud.android.view.adapters.MixedItemClickListener
import com.soundcloud.android.view.adapters.RecyclerViewParallaxer
import com.soundcloud.android.view.adapters.RepostEntityListObserver
import com.soundcloud.android.view.adapters.UpdatePlaylistListObserver
import com.soundcloud.android.view.adapters.UpdateTrackListObserver
import com.soundcloud.rx.eventbus.EventBusV2
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class StreamPresenter
@Inject
constructor(private val streamOperations: StreamOperations,
            private val adapter: StreamAdapter,
            private val imagePauseOnScrollListener: ImagePauseOnScrollListener,
            private val streamAdsController: StreamAdsController,
            private val streamDepthPublisherFactory: StreamDepthPublisher.Factory,
            private val eventBus: EventBusV2,
            itemClickListenerFactory: MixedItemClickListener.Factory,
            private val swipeRefreshAttacher: StreamSwipeRefreshAttacher,
            private val invitesDialogPresenter: FacebookInvitesDialogPresenter,
            private val navigationExecutor: NavigationExecutor,
            private val navigator: Navigator,
            private val newItemsIndicator: NewItemsIndicator,
            private val followingOperations: FollowingOperations,
            private val whyAdsDialogPresenter: WhyAdsDialogPresenter,
            private val videoSurfaceProvider: VideoSurfaceProvider,
            private val updatePlayableAdapterObserverFactory: UpdatePlayableAdapterObserver.Factory,
            streamMeasurementsFactory: StreamMeasurementsFactory) : TimelinePresenter<StreamItem>(swipeRefreshAttacher,
                                                                                                  RecyclerViewPresenter.Options.staggeredGrid(R.integer.grids_num_columns).build(),
                                                                                                  newItemsIndicator,
                                                                                                  streamOperations,
                                                                                                  adapter),
                                                                    NewItemsIndicator.Listener {
    private val itemClickListener: MixedItemClickListener = itemClickListenerFactory.create(Screen.STREAM, null)
    private val streamMeasurements: StreamMeasurements = streamMeasurementsFactory.create()

    private var streamDepthPublisher: StreamDepthPublisher? = null
    private val viewLifeCycleDisposable = CompositeDisposable()
    private var fragment: Fragment? = null
    private var hasFocus: Boolean = false

    override fun onCreate(fragment: Fragment, bundle: Bundle?) {
        super.onCreate(fragment, bundle)
        this.fragment = fragment
        binding.connect()
    }

    public override fun onBuildBinding(fragmentArgs: Bundle?): CollectionBinding<List<StreamItem>, StreamItem> {
        return CollectionBinding.fromV2(streamOperations.initialStreamItems().toObservable()
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .doOnSubscribe { streamMeasurements.startLoading() }
                                                .doOnNext { streamItems ->
                                                    handlePromotedImpression(streamItems)
                                                    adapter.clear()
                                                })
                .withAdapter(adapter)
                .withPager(streamOperations.pagingFunction())
                .addObserver(onNext { streamMeasurements.endLoading() })
                .build()
    }

    public override fun onRefreshBinding(): CollectionBinding<List<StreamItem>, StreamItem> {
        streamMeasurements.startRefreshing()
        newItemsIndicator.hideAndReset()
        return CollectionBinding.fromV2(streamOperations.updatedStreamItems().toObservable().doOnNext { this.handlePromotedImpression(it) })
                .withAdapter(adapter)
                .withPager(streamOperations.pagingFunction())
                .addObserver(onNext { streamMeasurements.endRefreshing() })
                .build()
    }

    override fun onViewCreated(fragment: Fragment, view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(fragment, view, savedInstanceState)
        streamAdsController.onViewCreated(recyclerView, adapter)

        val layoutManager = recyclerView.layoutManager as? StaggeredGridLayoutManager
        streamDepthPublisher = streamDepthPublisherFactory.create(layoutManager, hasFocus)

        configureEmptyView()
        addScrollListeners()
        handlePromotedImpression()
        viewLifeCycleDisposable.clear()
        viewLifeCycleDisposable.addAll(
                eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM, updatePlayableAdapterObserverFactory.create(adapter)),
                eventBus.subscribe(EventQueue.TRACK_CHANGED, UpdateTrackListObserver(adapter)),
                eventBus.subscribe(EventQueue.PLAYLIST_CHANGED, UpdatePlaylistListObserver(adapter)),
                eventBus.subscribe(EventQueue.LIKE_CHANGED, LikeEntityListObserver(adapter)),
                eventBus.subscribe(EventQueue.REPOST_CHANGED, RepostEntityListObserver(adapter)),
                eventBus.queue(EventQueue.STREAM)
                        .filter { it.isStreamRefreshed }
                        .flatMap { updateIndicatorFromMostRecent() }.subscribeWith(DefaultObserver()),
                followingOperations.onUserFollowed().subscribeWith(LambdaObserver.onNext { swipeRefreshAttacher.forceRefresh() }),
                followingOperations.onUserUnfollowed().subscribeWith(LambdaObserver.onNext { swipeRefreshAttacher.forceRefresh() }),
                adapter.facebookListenerInvitesLoadingResult().subscribeWith(LambdaObserver.onNext {
                    when (it) {
                        is FacebookLoadingResult.Click -> onListenerInvitesClicked(it.position)
                        is FacebookLoadingResult.Dismiss -> onListenerInvitesDismiss(it.position)
                        is FacebookLoadingResult.Load -> onListenerInvitesLoaded(it.hasPictures)
                    }
                }),
                adapter.facebookCreatorInvitesLoadingResult().subscribeWith(LambdaObserver.onNext {
                    when (it) {
                        is FacebookLoadingResult.Click -> onCreatorInvitesClicked(it.position)
                        is FacebookLoadingResult.Dismiss -> onCreatorInvitesDismiss(it.position)
                    }
                }),
                adapter.upsellLoadingResult().subscribeWith(LambdaObserver.onNext {
                    when (it) {
                        is UpsellLoadingResult.Click -> onUpsellItemClicked(it.context)
                        is UpsellLoadingResult.Dismiss -> onUpsellItemDismissed(it.position)
                        is UpsellLoadingResult.Create -> onUpsellItemCreated()
                    }
                }),
                adapter.videoAdItemClick().subscribeWith(LambdaObserver.onNext {
                    when (it) {
                        is AdItemResult.AdItemClick -> onAdItemClicked(it.adData)
                        is AdItemResult.WhyAdsClicked -> onWhyAdsClicked(it.context)
                        is AdItemResult.VideoFullscreenClick-> onVideoFullscreenClicked(it.videoAd)
                        is AdItemResult.VideoTextureBind-> onVideoTextureBind(it.textureView, it.viewabilityLayer, it.videoAd)
                    }
                }),
                adapter.appInstallClick().subscribeWith(LambdaObserver.onNext {
                    when (it) {
                        is AdItemResult.AdItemClick -> onAdItemClicked(it.adData)
                        is AdItemResult.WhyAdsClicked -> onWhyAdsClicked(it.context)
                        is AdItemResult.VideoFullscreenClick-> onVideoFullscreenClicked(it.videoAd)
                        is AdItemResult.VideoTextureBind-> onVideoTextureBind(it.textureView, it.viewabilityLayer, it.videoAd)
                    }
                })
        )
    }

    fun onFocusChange(hasFocus: Boolean) {
        this.hasFocus = hasFocus
        if (hasFocus) {
            streamAdsController.onFocusGain()
        } else {
            streamAdsController.onFocusLoss(true)
        }
        streamDepthPublisher?.onFocusChange(hasFocus)
        handlePromotedImpression()
    }

    private fun handlePromotedImpression(items: List<StreamItem> = adapter.items) {
        if (hasFocus) {
            streamOperations.publishPromotedImpression(items)
        }
    }

    override fun onPause(fragment: Fragment) {
        super.onPause(fragment)
        streamAdsController.onPause(fragment)
    }

    override fun onResume(fragment: Fragment) {
        super.onResume(fragment)
        streamAdsController.onResume(hasFocus)
    }

    private fun addScrollListeners() {
        recyclerView.addOnScrollListener(imagePauseOnScrollListener)
        recyclerView.addOnScrollListener(streamAdsController)
        recyclerView.addOnScrollListener(RecyclerViewParallaxer())
        recyclerView.addOnScrollListener(streamDepthPublisher)
    }

    override fun onDestroyView(fragment: Fragment) {
        streamAdsController.onDestroyView()

        streamDepthPublisher?.unsubscribe()
        streamDepthPublisher = null

        viewLifeCycleDisposable.clear()
        newItemsIndicator.destroy()
        recyclerView.removeOnScrollListener(imagePauseOnScrollListener)
        imagePauseOnScrollListener.resume()
        super.onDestroyView(fragment)
    }

    override fun onDestroy(fragment: Fragment) {
        if (fragment.activity.isChangingConfigurations) {
            videoSurfaceProvider.onConfigurationChange(Origin.STREAM)
        } else {
            videoSurfaceProvider.onDestroy(Origin.STREAM)
        }
        streamAdsController.onDestroy()
        super.onDestroy(fragment)
    }

    private fun configureEmptyView() {
        with(emptyView) {
            setImage(R.drawable.empty_stream)
            setMessageText(R.string.list_empty_stream_message)
            setActionText(R.string.list_empty_stream_action)
            setButtonActions(Intent(Actions.SEARCH))
        }
    }

    public override fun onItemClicked(view: View, position: Int) {
        val item = adapter.getItem(position)
        item.playableItem.ifPresent {
            if (item.isPromoted) {
                publishPromotedItemClickEvent(it)
            }
            itemClickListener.legacyOnPostClick(RxJava.toV1Observable<List<PlayableWithReposter>>(streamOperations.urnsForPlayback()), view, position, it)
        }
    }

    override fun handleError(error: Throwable): EmptyView.Status {
        return ErrorUtils.emptyViewStatusFromError(error)
    }

    private fun publishPromotedItemClickEvent(item: PlayableItem) {
        eventBus.publish<TrackingEvent>(EventQueue.TRACKING, PromotedTrackingEvent.forItemClick(item, Screen.STREAM.get()))
    }

    private fun onListenerInvitesClicked(position: Int) {
        val item = adapter.getItem(position)
        if (item is FacebookListenerInvites) {
            trackInvitesEvent(forListenerClick(item.hasPictures()))
            fragment?.let {
                invitesDialogPresenter.showForListeners(it.activity)
            }
            removeItem(position)
        }
    }

    private fun onListenerInvitesDismiss(position: Int) {
        val item = adapter.getItem(position)
        if (item is FacebookListenerInvites) {
            trackInvitesEvent(forListenerDismiss(item.hasPictures()))
            removeItem(position)
        }
    }

    private fun onListenerInvitesLoaded(hasPictures: Boolean) {
        trackInvitesEvent(forListenerShown(hasPictures))
    }

    private fun onCreatorInvitesClicked(position: Int) {
        val item = adapter.getItem(position)
        if (item is StreamItem.FacebookCreatorInvites) {
            if (Urn.NOT_SET != item.trackUrn) {
                trackInvitesEvent(forCreatorClick())
                fragment?.let {
                    invitesDialogPresenter.showForCreators(it.activity, item.trackUrl, item.trackUrn)
                }
            }
            removeItem(position)
        }
    }

    private fun onCreatorInvitesDismiss(position: Int) {
        if (adapter.getItem(position) is StreamItem.FacebookCreatorInvites) {
            trackInvitesEvent(forCreatorDismiss())
            removeItem(position)
        }
    }

    private fun onUpsellItemDismissed(position: Int) {
        streamOperations.disableUpsell()
        removeItem(position)
    }

    private fun onUpsellItemClicked(context: Context) {
        navigationExecutor.openUpgrade(context, UpsellContext.PREMIUM_CONTENT)
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forStreamClick())
    }

    private fun onUpsellItemCreated() {
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forStreamImpression())
    }

    private fun removeItem(position: Int) {
        adapter.removeItem(position)
        adapter.notifyItemRemoved(position)
    }

    private fun trackInvitesEvent(event: FacebookInvitesEvent) {
        eventBus.publish(EventQueue.TRACKING, event)
    }

    override fun getNewItemsTextResourceId() = R.plurals.stream_new_posts

    fun onAdItemClicked(adData: AdData) {
        when (adData) {
            is AppInstallAd -> UIEvent.fromAppInstallAdClickThrough(adData) to adData.clickThroughUrl()
            is VideoAd -> UIEvent.fromPlayableClickThrough(adData, TrackSourceInfo(Screen.STREAM.get(), true)) to adData.clickThroughUrl()
            else -> null
        }?.let { (event, url) ->
            eventBus.publish(EventQueue.TRACKING, event)
            navigator.navigateTo(NavigationTarget.forAdClickthrough(url))
        }
    }

    fun onWhyAdsClicked(context: Context) {
        whyAdsDialogPresenter.show(context)
    }

    fun onVideoTextureBind(textureView: TextureView, viewabilityLayer: View, videoAd: VideoAd) {
        if (!streamAdsController.isInFullscreen) {
            videoSurfaceProvider.setTextureView(videoAd.uuid(), Origin.STREAM, textureView, viewabilityLayer)
        }
    }

    fun onVideoFullscreenClicked(videoAd: VideoAd) {
        streamAdsController.setFullscreenEnabled()
        navigator.navigateTo(NavigationTarget.forFullscreenVideoAd(videoAd.adUrn()))
    }
}
