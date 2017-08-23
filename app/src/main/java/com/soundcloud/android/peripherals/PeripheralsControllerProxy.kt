package com.soundcloud.android.peripherals

import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.rx.observers.LambdaObserver
import com.soundcloud.android.utils.extensions.plusAssign
import com.soundcloud.rx.eventbus.EventBusV2
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject
import javax.inject.Provider

class PeripheralsControllerProxy
@Inject
constructor(internal val eventBus: EventBusV2,
            internal val controller: Provider<PeripheralsController>) {

    private val disposable = CompositeDisposable()

    fun subscribe() {
        disposable += eventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, LambdaObserver.onNext { controller.get().onCurrentUserChanged(it) })
        disposable += eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, LambdaObserver.onNext { controller.get().onPlayStateEvent(it) })
        disposable += eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM, LambdaObserver.onNext { controller.get().onCurrentPlayQueueItem(it) })
    }
}
