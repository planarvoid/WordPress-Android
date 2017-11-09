package com.soundcloud.android.stream

import android.content.Context
import android.view.TextureView
import android.view.View
import com.soundcloud.android.ads.AdData
import com.soundcloud.android.ads.AdItemCallback
import com.soundcloud.android.ads.AppInstallAd
import com.soundcloud.android.ads.StreamAdsController
import com.soundcloud.android.ads.VideoAd
import com.soundcloud.android.ads.WhyAdsDialogPresenter
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.events.FacebookInvitesEvent
import com.soundcloud.android.events.UIEvent
import com.soundcloud.android.events.UpgradeFunnelEvent
import com.soundcloud.android.facebookinvites.FacebookNotificationCallback
import com.soundcloud.android.main.Screen
import com.soundcloud.android.model.Urn
import com.soundcloud.android.navigation.NavigationTarget
import com.soundcloud.android.navigation.Navigator
import com.soundcloud.android.payments.UpsellContext
import com.soundcloud.android.playback.TrackSourceInfo
import com.soundcloud.android.playback.VideoSurfaceProvider
import com.soundcloud.android.rx.RxSignal
import com.soundcloud.android.rx.observers.LambdaObserver
import com.soundcloud.android.tracks.TieredTracks
import com.soundcloud.android.upsell.InlineUpsellOperations
import com.soundcloud.android.upsell.UpsellItemCallback
import com.soundcloud.android.utils.collection.AsyncLoader
import com.soundcloud.android.utils.collection.AsyncLoaderState
import com.soundcloud.android.view.BasePresenter
import com.soundcloud.android.view.BaseView
import com.soundcloud.java.optional.Optional
import com.soundcloud.rx.eventbus.EventBusV2
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject
import javax.inject.Provider

internal class StreamUniflowPresenter
@Inject
constructor(private val streamOperations: StreamUniflowOperations,
            private val eventBus: EventBusV2,
            private val navigator: Navigator,
            private val whyAdsDialogPresenter: WhyAdsDialogPresenter,
            private val streamAdsController: StreamAdsController,
            private val videoSurfaceProvider: VideoSurfaceProvider,
            private val upsellOperations: InlineUpsellOperations) : BasePresenter<List<StreamItem>, RxSignal, StreamUniflowView>() {

    private val shouldEmitNotificationItem = BehaviorSubject.createDefault(true)
    private val shouldAddUpsellItem = BehaviorSubject.createDefault(true)

    override fun attachView(view: StreamUniflowView) {
        super.attachView(view)
        compositeDisposable.addAll(
                view.facebookListenerInvitesItemCallback().subscribeWith(LambdaObserver.onNext {
                    when (it) {
                        is FacebookNotificationCallback.Click -> onListenerInvitesClicked(it.streamItem, view)
                        is FacebookNotificationCallback.Dismiss -> onListenerInvitesDismiss(it.streamItem)
                        is FacebookNotificationCallback.Load -> onListenerInvitesLoaded(it.hasPictures)
                    }
                }),
                view.facebookCreatorInvitesItemCallback().subscribeWith(LambdaObserver.onNext {
                    when (it) {
                        is FacebookNotificationCallback.Click -> onCreatorInvitesClicked(it.streamItem, view)
                        is FacebookNotificationCallback.Dismiss -> onCreatorInvitesDismiss()
                    }
                }),
                view.upsellItemCallback().subscribeWith(LambdaObserver.onNext {
                    when (it) {
                        is UpsellItemCallback.Click -> onUpsellItemClicked()
                        is UpsellItemCallback.Dismiss -> onUpsellItemDismissed()
                        is UpsellItemCallback.Create -> onUpsellItemCreated()
                    }
                }),
                view.videoAdItemCallback().subscribeWith(LambdaObserver.onNext {
                    when (it) {
                        is AdItemCallback.AdItemClick -> onAdItemClicked(it.adData)
                        is AdItemCallback.WhyAdsClicked -> onWhyAdsClicked(it.context)
                        is AdItemCallback.VideoFullscreenClick -> onVideoFullscreenClicked(it.videoAd)
                        is AdItemCallback.VideoTextureBind -> onVideoTextureBind(it.textureView, it.viewabilityLayer, it.videoAd)
                    }
                }),
                view.appInstallCallback().subscribeWith(LambdaObserver.onNext {
                    when (it) {
                        is AdItemCallback.AdItemClick -> onAdItemClicked(it.adData)
                        is AdItemCallback.WhyAdsClicked -> onWhyAdsClicked(it.context)
                        is AdItemCallback.VideoFullscreenClick -> onVideoFullscreenClicked(it.videoAd)
                        is AdItemCallback.VideoTextureBind -> onVideoTextureBind(it.textureView, it.viewabilityLayer, it.videoAd)
                    }
                }))
    }

    override fun firstPageFunc(pageParams: RxSignal): Observable<AsyncLoader.PageResult<List<StreamItem>>> {
        return streamOperations.initialStreamItems()
                .observeOn(AndroidSchedulers.mainThread())
                .map {
                    AsyncLoader.PageResult.from(it, nextPage(it))
                }.toObservable()
    }

    override fun refreshFunc(pageParams: RxSignal): Observable<AsyncLoader.PageResult<List<StreamItem>>> {
        return streamOperations.updatedStreamItems()
                .observeOn(AndroidSchedulers.mainThread())
                .map {
                    AsyncLoader.PageResult.from(it, nextPage(it))
                }.toObservable()
    }

    override fun postProcessViewModel(viewModel: List<StreamItem>): Observable<List<StreamItem>> {
        return shouldEmitNotificationItem
                .flatMap { shouldEmitNotificationItem ->
                    if (shouldEmitNotificationItem) {
                        streamOperations.initialNotificationItem().map { notificationItem ->
                            addFirstItem(viewModel, notificationItem)
                        }.toSingle(viewModel).toObservable()
                    } else {
                        Observable.just(viewModel)
                    }
                }
                .flatMap { streamItems ->
                    shouldAddUpsellItem.map {
                        if (it) {
                            addUpsellableItem(streamItems.toMutableList())
                        } else {
                            streamItems
                        }
                    }
                }
    }

    private fun addFirstItem(viewModel: List<StreamItem>, notificationItem: StreamItem): List<StreamItem> {
        with(viewModel.toMutableList()) {
            this.add(0, notificationItem)
            return this
        }
    }

    private fun nextPage(currentPage: List<StreamItem>): Optional<Provider<Observable<AsyncLoader.PageResult<List<StreamItem>>>>> {
        return Optional.fromNullable(streamOperations.nextPageItems(currentPage)?.toObservable()?.let { Provider { it.map { AsyncLoader.PageResult.from(it, nextPage(it)) } } })
    }

    fun scrollToTop() {
        //        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun onFocusChange(hasFocus: Boolean) {

    }

    private fun onListenerInvitesClicked(item: StreamItem.FacebookListenerInvites, view: StreamUniflowView) {
        eventBus.publish(EventQueue.TRACKING, FacebookInvitesEvent.forListenerClick(item.hasPictures()))
        view.showFacebookListenersInvitesDialog()
        shouldEmitNotificationItem.onNext(false)
    }

    private fun onListenerInvitesDismiss(item: StreamItem.FacebookListenerInvites) {
        eventBus.publish(EventQueue.TRACKING, FacebookInvitesEvent.forListenerDismiss(item.hasPictures()))
        shouldEmitNotificationItem.onNext(false)
    }

    private fun onListenerInvitesLoaded(hasPictures: Boolean) {
        eventBus.publish(EventQueue.TRACKING, FacebookInvitesEvent.forListenerShown(hasPictures))
    }

    private fun onCreatorInvitesClicked(item: StreamItem.FacebookCreatorInvites, view: StreamUniflowView) {
        if (Urn.NOT_SET != item.trackUrn) {
            eventBus.publish(EventQueue.TRACKING, FacebookInvitesEvent.forCreatorClick())
            view.showFacebookCreatorsInvitesDialog(item.trackUrl, item.trackUrn)
        }
        shouldEmitNotificationItem.onNext(false)
    }

    private fun onCreatorInvitesDismiss() {
        eventBus.publish(EventQueue.TRACKING, FacebookInvitesEvent.forCreatorDismiss())
        shouldEmitNotificationItem.onNext(false)
    }

    private fun addUpsellableItem(streamItems: MutableList<StreamItem>): List<StreamItem> {
        if (upsellOperations.shouldDisplayInStream()) {
            getFirstUpsellable(streamItems)?.let {
                streamItems.add(streamItems.indexOf(it) + 1, StreamItem.Upsell)
            }
        }
        return streamItems
    }

    private fun getFirstUpsellable(streamItems: List<StreamItem>): StreamItem? {
        return streamItems.firstOrNull { it is TrackStreamItem && TieredTracks.isHighTierPreview(it.trackItem) }
    }

    private fun onUpsellItemDismissed() {
        upsellOperations.disableInStream()
        shouldAddUpsellItem.onNext(false)
    }

    private fun onUpsellItemClicked() {
        navigator.navigateTo(NavigationTarget.forUpgrade(UpsellContext.PREMIUM_CONTENT))
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forStreamClick())
    }

    private fun onUpsellItemCreated() {
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forStreamImpression())
    }

    private fun onAdItemClicked(adData: AdData) {
        when (adData) {
            is AppInstallAd -> UIEvent.fromAppInstallAdClickThrough(adData) to adData.clickThroughUrl()
            is VideoAd -> UIEvent.fromPlayableClickThrough(adData, TrackSourceInfo(Screen.STREAM.get(), true)) to adData.clickThroughUrl()
            else -> null
        }?.let { (event, url) ->
            eventBus.publish(EventQueue.TRACKING, event)
            navigator.navigateTo(NavigationTarget.forAdClickthrough(url))
        }
    }

    private fun onWhyAdsClicked(context: Context) {
        whyAdsDialogPresenter.show(context)
    }

    private fun onVideoTextureBind(textureView: TextureView, viewabilityLayer: View, videoAd: VideoAd) {
        if (!streamAdsController.isInFullscreen) {
            videoSurfaceProvider.setTextureView(videoAd.uuid(), VideoSurfaceProvider.Origin.STREAM, textureView, viewabilityLayer)
        }
    }

    private fun onVideoFullscreenClicked(videoAd: VideoAd) {
        streamAdsController.setFullscreenEnabled()
        navigator.navigateTo(NavigationTarget.forFullscreenVideoAd(videoAd.adUrn()))
    }
}

internal interface StreamUniflowView : BaseView<AsyncLoaderState<List<StreamItem>>, RxSignal> {
    fun facebookListenerInvitesItemCallback(): Observable<FacebookNotificationCallback<StreamItem.FacebookListenerInvites>>
    fun facebookCreatorInvitesItemCallback(): Observable<FacebookNotificationCallback<StreamItem.FacebookCreatorInvites>>
    fun upsellItemCallback(): Observable<UpsellItemCallback>
    fun videoAdItemCallback(): Observable<AdItemCallback>
    fun appInstallCallback(): Observable<AdItemCallback>
    fun showFacebookListenersInvitesDialog()
    fun showFacebookCreatorsInvitesDialog(trackUrl: String, trackUrn: Urn)
}
