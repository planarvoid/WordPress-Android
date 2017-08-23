package com.soundcloud.android.playback

import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.rx.observers.LambdaObserver
import com.soundcloud.android.utils.extensions.plusAssign
import com.soundcloud.rx.eventbus.EventBusV2
import dagger.Lazy
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class PlayPublisherProxy
@Inject
constructor(internal val eventBus: EventBusV2,
            internal val controller: Lazy<PlayPublisher>) {

    private val disposable = CompositeDisposable()

    fun subscribe() {
        disposable += eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED)
                .filter { event -> isPlayerPlaying(event) }
                .subscribeWith(LambdaObserver.onNext { controller.get().onPlaybackStateChanged(it) })
    }

    private fun isPlayerPlaying(event: PlayStateEvent) = !event.playingItemUrn.isAd && event.isPlayerPlaying

}
