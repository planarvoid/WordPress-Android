package com.soundcloud.android.playback.widget

import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.rx.observers.LambdaObserver
import com.soundcloud.android.utils.extensions.plusAssign
import com.soundcloud.rx.eventbus.EventBusV2
import dagger.Lazy
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerWidgetControllerProxy
@Inject
constructor(internal val eventBus: EventBusV2,
            internal val controller: Lazy<PlayerWidgetController>) {

    private val disposable = CompositeDisposable()

    fun subscribe() {
        disposable += eventBus.subscribe(EventQueue.TRACK_CHANGED, LambdaObserver.onNext { controller.get().onTrackMetadataChange(it) })
        disposable += eventBus.subscribe(EventQueue.LIKE_CHANGED, LambdaObserver.onNext { controller.get().onTrackLikeChange(it) })
        disposable += eventBus.subscribe(EventQueue.REPOST_CHANGED, LambdaObserver.onNext { controller.get().onTrackRepostChange(it) })
        disposable += eventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, LambdaObserver.onNext { controller.get().onCurrentUserChanged(it) })
        disposable += eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, LambdaObserver.onNext { controller.get().onPlaybackStateUpdate(it) })
        disposable += eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM, LambdaObserver.onNext { controller.get().onCurrentItemChange(it) })
    }
}
