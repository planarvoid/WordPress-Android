package com.soundcloud.android.ads

import com.soundcloud.android.events.ActivityLifeCycleEvent
import com.soundcloud.android.events.AdOverlayEvent
import com.soundcloud.android.events.CurrentPlayQueueItemEvent
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.events.PlayerUIEvent
import com.soundcloud.android.rx.observers.LambdaObserver
import com.soundcloud.android.utils.extensions.plusAssign
import com.soundcloud.rx.eventbus.EventBusV2
import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function3
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerAdsControllerProxy
@Inject
constructor(internal val eventBus: EventBusV2,
            internal val controller: Lazy<PlayerAdsController>) {

    private val disposables = CompositeDisposable()

    fun subscribe() {
        disposables += eventBus.subscribe(EventQueue.ACTIVITY_LIFE_CYCLE, LambdaObserver.onNext { controller.get().onActivityLifeCycleEvent(it) })
        disposables += eventBus.subscribe(EventQueue.PLAYER_UI, LambdaObserver.onNext { controller.get().onPlayerState(it) })
        disposables += eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM, LambdaObserver.onNext { controller.get().onCurrentPlayQueueItem(it) })
        disposables += eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, LambdaObserver.onNext { controller.get().onPlaybackStateChanged(it) })
        disposables += eventBus.subscribe(EventQueue.AD_OVERLAY, LambdaObserver.onNext { controller.get().onAdOverlayEvent(it) })

        disposables += Observable.merge(
                eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM),
                eventBus.queue(EventQueue.PLAY_QUEUE).filter { it.isQueueUpdate }
        ).subscribe { controller.get().onQueueChangeForAd() }


        disposables += listenToVisualAdImpressions()
        disposables += listenToAdOverlayImpressions()
    }

    private fun listenToAdOverlayImpressions(): Disposable {
        return Observable.combineLatest(
                eventBus.queue(EventQueue.ACTIVITY_LIFE_CYCLE),
                eventBus.queue(EventQueue.PLAYER_UI),
                eventBus.queue(EventQueue.AD_OVERLAY),
                adOverlayCombineFunction()
        ).subscribeWith(LambdaObserver.onNext<AdOverlayImpressionState> { controller.get().onAdOverlayImpressionState(it) })
    }

    private fun listenToVisualAdImpressions(): Disposable {
        return Observable.combineLatest(
                eventBus.queue(EventQueue.ACTIVITY_LIFE_CYCLE),
                eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM)
                        .filter { it.currentPlayQueueItem.isAudioAd }
                        .doOnNext { controller.get().unlockVisualAdImpression() },
                eventBus.queue(EventQueue.PLAYER_UI),
                visualAdCombineFunction()
        ).subscribeWith(LambdaObserver.onNext { controller.get().onVisualAdImpressionState(it) })
    }

    private fun adOverlayCombineFunction(): Function3<ActivityLifeCycleEvent, PlayerUIEvent, AdOverlayEvent, AdOverlayImpressionState> {
        return Function3 { event, playerUiEvent, adOverlayEvent ->
            AdOverlayImpressionState(
                    adOverlayEvent.kind == AdOverlayEvent.SHOWN,
                    event.kind == ActivityLifeCycleEvent.ON_RESUME_EVENT,
                    playerUiEvent.kind == PlayerUIEvent.PLAYER_EXPANDED,
                    adOverlayEvent.currentPlayingUrn,
                    adOverlayEvent.adData,
                    adOverlayEvent.trackSourceInfo)
        }
    }

    private fun visualAdCombineFunction(): Function3<ActivityLifeCycleEvent, CurrentPlayQueueItemEvent, PlayerUIEvent, VisualAdImpressionState> {
        return Function3 { event, currentItemEvent, playerUIEvent ->
            VisualAdImpressionState(
                    currentItemEvent.currentPlayQueueItem.adData.get(),
                    event.kind == ActivityLifeCycleEvent.ON_RESUME_EVENT,
                    playerUIEvent.kind == PlayerUIEvent.PLAYER_EXPANDED)
        }
    }
}
